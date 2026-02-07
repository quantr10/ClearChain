using System.Net;
using System.Text.Json;
using ClearChain.API.DTOs.Common;

namespace ClearChain.API.Middleware;

public class ErrorHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ErrorHandlingMiddleware> _logger;

    public ErrorHandlingMiddleware(RequestDelegate next, ILogger<ErrorHandlingMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await _next(context);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "An unhandled exception occurred");
            await HandleExceptionAsync(context, ex);
        }
    }

    private static async Task HandleExceptionAsync(HttpContext context, Exception exception)
    {
        context.Response.ContentType = "application/json";

        var response = exception switch
        {
            ArgumentException => new
            {
                statusCode = (int)HttpStatusCode.BadRequest,
                message = exception.Message,
                type = "ValidationError"
            },
            UnauthorizedAccessException => new
            {
                statusCode = (int)HttpStatusCode.Unauthorized,
                message = "Unauthorized access",
                type = "AuthenticationError"
            },
            KeyNotFoundException => new
            {
                statusCode = (int)HttpStatusCode.NotFound,
                message = exception.Message,
                type = "NotFoundError"
            },
            _ => new
            {
                statusCode = (int)HttpStatusCode.InternalServerError,
                message = "An error occurred while processing your request",
                type = "InternalError"
            }
        };

        context.Response.StatusCode = response.statusCode;

        var jsonResponse = JsonSerializer.Serialize(new
        {
            success = false,
            response.message,
            response.type,
            timestamp = DateTime.UtcNow
        });

        await context.Response.WriteAsync(jsonResponse);
    }
}