package com.rendezvous.room.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.repository.RoomMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomAccessService {

    private final RoomMemberRepository members;

    public RoomAccessService(RoomMemberRepository members) {
        this.members = members;
    }

    @Transactional(readOnly = true)
    public MemberContext requireMember(UUID roomId, UUID userId) {
        RoomMember member = members.findByRoomIdAndUserId(roomId, userId)
                .filter(found -> found.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        return new MemberContext(member.getRoom(), member);
    }
}
