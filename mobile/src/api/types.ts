import type { components } from './schema';

type Schemas = components['schemas'];

export type AuthResponse = Schemas['AuthResponse'];
export type UserResponse = Schemas['UserResponse'];
export type RegisterRequest = Schemas['RegisterRequest'];
export type LoginRequest = Schemas['LoginRequest'];
export type RefreshRequest = Schemas['RefreshRequest'];
export type LogoutRequest = Schemas['LogoutRequest'];
export type UpdateProfileRequest = Schemas['UpdateProfileRequest'];

export type RoomDetailsResponse = Schemas['RoomDetailsResponse'];
export type RoomSummaryResponse = Schemas['RoomSummaryResponse'];
export type CreateRoomRequest = Schemas['CreateRoomRequest'];
export type UpdateRoomRequest = Schemas['UpdateRoomRequest'];
export type JoinRoomRequest = Schemas['JoinRoomRequest'];
export type OwnerResponse = Schemas['OwnerResponse'];
export type PageResponseRoomSummaryResponse = Schemas['PageResponseRoomSummaryResponse'];

export type MemberResponse = Schemas['MemberResponse'];
export type ChangeRoleRequest = Schemas['ChangeRoleRequest'];

export type GeofenceRequest = Schemas['GeofenceRequest'];
export type GeofenceResponse = Schemas['GeofenceResponse'];

export type AlertResponse = Schemas['AlertResponse'];
export type PageResponseAlertResponse = Schemas['PageResponseAlertResponse'];

export type PositionResponse = Schemas['PositionResponse'];
export type TrailPointResponse = Schemas['TrailPointResponse'];
export type LocationPointRequest = Schemas['LocationPointRequest'];
export type LocationBatchRequest = Schemas['LocationBatchRequest'];
export type IngestResponse = Schemas['IngestResponse'];

export type AdminUserResponse = Schemas['AdminUserResponse'];
export type AdminRoomResponse = Schemas['AdminRoomResponse'];
export type PageResponseAdminUserResponse = Schemas['PageResponseAdminUserResponse'];
export type PageResponseAdminRoomResponse = Schemas['PageResponseAdminRoomResponse'];

export type RoomStatus = RoomDetailsResponse['status'];
export type RoomRole = MemberResponse['role'];
export type VisibilityMode = RoomDetailsResponse['visibilityMode'];
export type AlertType = AlertResponse['type'];
