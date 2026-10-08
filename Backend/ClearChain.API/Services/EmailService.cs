using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;

namespace ClearChain.API.Services;

public interface IEmailService
{
    Task SendVerificationEmailAsync(string toEmail, string toName, string code);
    Task SendPasswordResetEmailAsync(string toEmail, string toName, string code);
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

    public Task SendVerificationEmailAsync(string toEmail, string toName, string code) =>
        SendCodeEmailAsync(
            toEmail, toName, code,
            subject: "Verify your ClearChain account",
            heading: "Welcome to ClearChain!",
            intro: "Use the code below to verify your email address.",
            footer: "If you didn't create a ClearChain account, ignore this email.",
            kind: "Verification");

    public Task SendPasswordResetEmailAsync(string toEmail, string toName, string code) =>
        SendCodeEmailAsync(
            toEmail, toName, code,
            subject: "Reset your ClearChain password",
            heading: "Reset your password",
            intro: "Use the code below to choose a new password.",
            footer: "If you didn't ask to reset your password, ignore this email — your password stays the same.",
            kind: "Password reset");

    private async Task SendCodeEmailAsync(
        string toEmail, string toName, string code,
        string subject, string heading, string intro, string footer, string kind)
    {
        var host = _config["SMTP_HOST"];
        var port = int.Parse(_config["SMTP_PORT"] ?? "587");
        var user = _config["SMTP_USER"];
        var pass = _config["SMTP_PASS"];
        var from = _config["SMTP_FROM"] ?? user;

        if (string.IsNullOrWhiteSpace(host) ||
            string.IsNullOrWhiteSpace(user) ||
            string.IsNullOrWhiteSpace(pass) ||
            string.IsNullOrWhiteSpace(from))
        {
            _logger.LogWarning("SMTP not configured — skipping {Kind} email to {Email}", kind, toEmail);
            return;
        }

        var message = new MimeMessage();
        message.From.Add(new MailboxAddress("ClearChain", from));
        message.To.Add(new MailboxAddress(toName, toEmail));
        message.Subject = subject;
        message.Body = new TextPart("html")
        {
            Text = $"""
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#6750A4">{heading}</h2>
                  <p>Hi {System.Net.WebUtility.HtmlEncode(toName)},</p>
                  <p>{intro} It expires in <strong>15 minutes</strong>.</p>
                  <div style="font-size:36px;font-weight:bold;letter-spacing:12px;
                              text-align:center;padding:24px;background:#F3EDF7;
                              border-radius:12px;margin:24px 0">{code}</div>
                  <p style="color:#888;font-size:13px">{footer}</p>
                </div>
                """
        };

        using var client = new SmtpClient();
        await client.ConnectAsync(host, port, SecureSocketOptions.StartTls);
        await client.AuthenticateAsync(user, pass);
        await client.SendAsync(message);
        await client.DisconnectAsync(true);

        _logger.LogInformation("{Kind} email sent to {Email}", kind, toEmail);
    }
}
