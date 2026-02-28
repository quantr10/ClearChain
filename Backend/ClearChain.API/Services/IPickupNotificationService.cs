using ClearChain.API.DTOs.PickupRequests;

namespace ClearChain.API.Services;

public interface IPickupNotificationService
{
    Task NotifyPickupRequestCreated(PickupRequestData request);
    Task NotifyPickupRequestStatusChanged(PickupRequestData request, string oldStatus);
    Task NotifyPickupRequestCancelled(PickupRequestData request);
}