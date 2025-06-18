package kr.swyp.backend.friend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.common.dto.FileDto.FileUploadRequest;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendCheckingLog;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.domain.FriendDetail;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendRelation;
import kr.swyp.backend.friend.enums.FriendRemind;
import kr.swyp.backend.friend.enums.FriendSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

public class FriendDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FriendCreateListRequest {

        List<FriendRequest> friendList;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class FriendRequest {

            @NotNull(message = "친구 이름은 필수입니다.")
            private String name;

            @NotNull(message = "친구 출처는 필수입니다.")
            private FriendSource source;

            @NotNull(message = "연락 주기는 필수입니다.")
            private FriendContactFrequency contactFrequency;

            private FileUploadRequest imageUploadRequest;

            private FriendAnniversaryCreateRequest anniversary;

            private String phone;

            @JsonFormat(pattern = "yyyy-MM-dd")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            private LocalDate birthDay;

            private FriendRelation relation;

            private String memo;

            @Getter
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            public static class FriendAnniversaryCreateRequest {

                @NotNull(message = "기념일 이름은 필수입니다.")
                private String title;

                @JsonFormat(pattern = "yyyy-MM-dd")
                @DateTimeFormat(pattern = "yyyy-MM-dd")
                @NotNull(message = "기념일 날짜는 필수입니다.")
                private LocalDate date;

                public static FriendAnniversary toEntity(UUID friendId,
                        FriendAnniversaryCreateRequest request) {
                    return FriendAnniversary.builder()
                            .friendId(friendId)
                            .title(request.getTitle())
                            .date(request.getDate())
                            .build();
                }
            }

            public LocalDate getNextContactAt(FriendContactFrequency frequency) {
                LocalDate now = LocalDate.now();
                DayOfWeek targetDayOfWeek = frequency.getDayOfWeek();

                LocalDate calculatedDate = switch (frequency.getContactWeek()) {
                    case EVERY_DAY -> now.plusDays(1);
                    case EVERY_WEEK -> now.plusWeeks(1);
                    case EVERY_TWO_WEEK -> now.plusWeeks(2);
                    case EVERY_MONTH -> now.plusMonths(1);
                    case EVERY_SIX_MONTH -> now.plusMonths(6);
                    default ->
                            throw new IllegalArgumentException("연락 챙김 주기가 올바르지 않습니다," + frequency);
                };

                // 매일 주기가 아닌 경우 특정 요일로 조정
                if (frequency.getContactWeek() != FriendContactWeek.EVERY_DAY
                        && targetDayOfWeek != null) {
                    // 계산된 날짜의 요일
                    DayOfWeek calculatedDayOfWeek = calculatedDate.getDayOfWeek();

                    // 목표 요일까지 날짜 조정 (요일 간의 차이를 계산)
                    int daysToAdd = targetDayOfWeek.getValue() - calculatedDayOfWeek.getValue();

                    // 음수인 경우 다음 주로 이동
                    if (daysToAdd < 0) {
                        daysToAdd += 7;
                    }

                    // 요일 조정
                    calculatedDate = calculatedDate.plusDays(daysToAdd);
                }

                return calculatedDate;
            }
        }
    }

    @Getter
    @Builder
    public static class FriendCreateListResponse {

        private List<FriendResponse> friendList;

        @Getter
        @Builder
        public static class FriendResponse {

            private UUID friendId;
            private String name;
            private FriendSource source;
            private FriendContactFrequency contactFrequency;
            @JsonFormat(pattern = "yyyy-MM-dd")
            private LocalDate nextContactAt;
            private String phone;
            private String preSignedImageUrl;
            private String fileName;
            private String memo;
            private FriendAnniversaryCreateResponse anniversary;

            @Getter
            @Builder
            public static class FriendAnniversaryCreateResponse {

                private Long id;
                private String title;
                @JsonFormat(pattern = "yyyy-MM-dd")
                private LocalDate date;

                public static FriendAnniversaryCreateResponse fromEntity(FriendAnniversary entity) {
                    return FriendAnniversaryCreateResponse.builder()
                            .id(entity.getId())
                            .title(entity.getTitle())
                            .date(entity.getDate())
                            .build();
                }
            }
        }
    }

    @Getter
    @Builder
    public static class FriendCheckLogResponse {

        private Boolean isChecked;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        public static FriendCheckLogResponse fromEntity(FriendCheckingLog log) {
            return FriendCheckLogResponse.builder()
                    .isChecked(log.getIsChecked())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FriendListResponse {

        private UUID friendId;
        private Integer position;
        private String source;
        private String name;
        private String imageUrl;
        private String fileName;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate lastContactAt;

        public static FriendListResponse fromEntity(Friend friend, String imageUrl,
                String fileName, FriendCheckingLog friendCheckingLog) {
            return FriendListResponse.builder()
                    .friendId(friend.getFriendId())
                    .position(friend.getPosition())
                    .source(friend.getFriendSource().name())
                    .name(friend.getName())
                    .imageUrl(imageUrl)
                    .fileName(fileName)
                    .lastContactAt(
                            friendCheckingLog != null ? friendCheckingLog.getCreatedAt()
                                    .toLocalDate() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FriendPositionUpdateRequest {

        private Integer newPosition;
    }

    @Getter
    @Builder
    public static class FriendDetailResponse {

        private UUID friendId;
        private String imageUrl;
        private FriendRelation relation;
        private String name;
        private FriendContactFrequency contactFrequency;
        private LocalDate birthday;
        private List<FriendAnniversaryDetailResponse> anniversaryList;
        private String memo;
        private String phone;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate lastContactAt;

        @Getter
        @Builder
        public static class FriendAnniversaryDetailResponse {

            private Long id;
            private String title;
            @JsonFormat(pattern = "yyyy-MM-dd")
            private LocalDate date;

            public static FriendAnniversaryDetailResponse fromEntity(FriendAnniversary entity) {
                return FriendAnniversaryDetailResponse.builder()
                        .id(entity.getId())
                        .title(entity.getTitle())
                        .date(entity.getDate())
                        .build();
            }
        }

        public static FriendDetailResponse fromEntity(Friend friend, String imageUrl,
                FriendDetail detail, List<FriendAnniversary> friendAnniversaryList,
                FriendCheckingLog friendCheckingLog) {
            return FriendDetailResponse.builder()
                    .friendId(friend.getFriendId())
                    .imageUrl(imageUrl)
                    .relation(detail.getRelation())
                    .name(friend.getName())
                    .contactFrequency(friend.getContactFrequency())
                    .birthday(detail.getBirthday())
                    .anniversaryList(friendAnniversaryList.stream()
                            .map(FriendAnniversaryDetailResponse::fromEntity)
                            .toList())
                    .memo(detail.getMemo())
                    .phone(detail.getPhone())
                    .lastContactAt(
                            friendCheckingLog != null ? friendCheckingLog.getCreatedAt()
                                    .toLocalDate() : null)
                    .build();
        }

        public static FriendDetailResponse fromEntity(Friend friend, String imageUrl,
                FriendDetail detail, List<FriendAnniversary> friendAnniversaryList) {
            return FriendDetailResponse.builder()
                    .friendId(friend.getFriendId())
                    .imageUrl(imageUrl)
                    .relation(detail.getRelation())
                    .name(friend.getName())
                    .contactFrequency(friend.getContactFrequency())
                    .birthday(detail.getBirthday())
                    .anniversaryList(friendAnniversaryList.stream()
                            .map(FriendAnniversaryDetailResponse::fromEntity)
                            .toList())
                    .memo(detail.getMemo())
                    .phone(detail.getPhone())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FriendDetailUpdateRequest {

        private String name;
        private FriendRelation relation;
        private FriendContactFrequency contactFrequency;
        @JsonFormat(pattern = "yyyy-MM-dd")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate birthday;
        private List<FriendAnniversaryDetailUpdateRequest> anniversaryList;
        private String memo;
        private String phone;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class FriendAnniversaryDetailUpdateRequest {

            private Long id;
            private String title;
            @JsonFormat(pattern = "yyyy-MM-dd")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            private LocalDate date;
        }
    }

    @Getter
    @Builder
    public static class FriendNearResponse {

        private UUID friendId;
        private String name;
        private FriendRemind type;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate nextContactAt;
    }
}
