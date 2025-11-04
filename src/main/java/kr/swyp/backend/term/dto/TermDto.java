package kr.swyp.backend.term.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import kr.swyp.backend.member.domain.MemberTermsAgreement;
import kr.swyp.backend.term.domain.Term;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TermDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermResponse {

        private Long termId;
        private String title;
        private String version;
        private Boolean isRequired;

        public static TermResponse fromEntity(Term term) {
            return TermResponse.builder()
                    .termId(term.getId())
                    .title(term.getTitle())
                    .version(term.getVersion())
                    .isRequired(term.getIsRequired())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermAgreementRequest {

        @NotNull(message = "약관 ID는 필수입니다.")
        private Long termId;

        @NotNull(message = "동의 여부는 필수입니다.")
        private Boolean isAgreed;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermsAgreementRequest {

        @NotEmpty(message = "약관 동의 목록은 비어있을 수 없습니다.")
        private List<TermAgreementRequest> agreements;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermAgreementResponse {

        private Long termId;
        private String title;
        private String version;
        private Boolean isRequired;
        private Boolean isAgreed;
        private LocalDateTime agreedAt;

        public static TermAgreementResponse fromEntity(Term term,
                MemberTermsAgreement agreement) {
            return TermAgreementResponse.builder()
                    .termId(term.getId())
                    .title(term.getTitle())
                    .version(term.getVersion())
                    .isRequired(term.getIsRequired())
                    .isAgreed(agreement != null && agreement.getIsAgreed())
                    .agreedAt(agreement != null ? agreement.getCreatedAt() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermsAgreementResponse {

        private List<TermAgreementResponse> agreements;

        public static TermsAgreementResponse of(List<TermAgreementResponse> agreements) {
            return TermsAgreementResponse.builder()
                    .agreements(agreements)
                    .build();
        }
    }
}
