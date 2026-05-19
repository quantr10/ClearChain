using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;

namespace ClearChain.API.Services;

public interface IEmailService
{
    Task SendVerificationEmailAsync(string toEmail, string toName, string code);
}

public class EmailService : IEmailService
{
    private readonly IConfiguration _config;
    private readonly ILogger<EmailService> _logger;

    public EmailService(IConfiguration config, ILogger<EmailService> logger)
    {
        _config = config;
        _logger = logger;
    }

    public async Task SendVerificationEmailAsync(string toEmail, string toName, string code)
    {
        var host = _config["SMTP_HOST"];
        var port = int.Parse(_config["SMTP_PORT"] ?? "587");
        var user = _config["SMTP_USER"];
        var pass = _config["SMTP_PASS"];
        var from = _config["SMTP_FROM"] ?? user;

        if (string.IsNullOrWhiteSpace(host) || string.IsNullOrWhiteSpace(user))
        {
            _logger.LogWarning("SMTP not configured — skipping verification email to {Email}", toEmail);
            return;
        }

        var message = new MimeMessage();
        message.From.Add(new MailboxAddress("ClearChain", from));
        message.To.Add(new MailboxAddress(toName, toEmail));
        message.Subject = "Verify your ClearChain account";
        message.Body = new TextPart("html")
        {
            Text = $"""
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#6750A4">Welcome to ClearChain!</h2>
                  <p>Hi {toName},</p>
                  <p>Use the code below to verify your email address. It expires in <strong>15 minutes</strong>.</p>
                  <div style="font-size:36px;font-weight:bold;letter-spacing:12px;
                              text-align:center;padding:24px;background:#F3EDF7;
                              border-radius:12px;margin:24px 0">{code}</div>
                  <p style="color:#888;font-size:13px">If you didn't create a ClearChain account, ignore this email.</p>
                </div>
                """
        };

        using var client = new SmtpClient();
        await client.ConnectAsync(host, port, SecureSocketOptions.StartTls);
        await client.AuthenticateAsync(user, pass);
        await client.SendAsync(message);
        await client.DisconnectAsync(true);

        _logger.LogInformation("Verification email sent to {Email}", toEmail);
    }
}
