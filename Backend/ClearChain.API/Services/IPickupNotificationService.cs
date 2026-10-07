using ClearChain.API.DTOs.PickupRequests;

namespace ClearChain.API.Services;

public interface IPickupNotificationService
{
    Task NotifyPickupRequestCreatedAsync(PickupRequestData request);
    Task NotifyPickupRequestStatusChangedAsync(PickupRequestData request);
    Task NotifyPickupRequestCancelledAsync(PickupRequestData request);
}
