package life.hanyang.core.playlist.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import life.hanyang.core.playlist.domain.Genre;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistSongCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private PlaylistSongCreateRequest createRequestWithAlbumArt(String albumArtUrl) {
        return new PlaylistSongCreateRequest(
                "4cOdK2wGLETKBW3PvgPWqT",
                "Ditto",
                "NewJeans",
                albumArtUrl,
                "과제할 때 듣기 좋아요",
                UUID.randomUUID(),
                Set.of(Genre.KPOP)
        );
    }

    @Test
    @DisplayName("정상 스포티파이 이미지 URL 1번 검증 통과")
    void validSpotifyImage1_Success() {
        // given
        String validUrl1 = "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290";
        PlaylistSongCreateRequest request = createRequestWithAlbumArt(validUrl1);

        // when
        Set<ConstraintViolation<PlaylistSongCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("정상 스포티파이 이미지 URL 2번 검증 통과")
    void validSpotifyImage2_Success() {
        // given
        String validUrl2 = "https://i.scdn.co/image/ab67616d0000b273951f05b855b09c8b4d7d2ee5";
        PlaylistSongCreateRequest request = createRequestWithAlbumArt(validUrl2);

        // when
        Set<ConstraintViolation<PlaylistSongCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd29",   // 39자리 (글자 1개 빠짐)
            "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd2900",  // 41자리 (글자 1개 늘어남)
            "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd29G",  // 16진수가 아닌 문자(G) 포함
            "https://imgur.com/ab67616d0000b273bd15713cf9824b7842bcd290.jpg",    // 다른 도메인
            "http://evil.com/porn.jpg",                                            // 악의적 외부 이미지
            "ftp://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290",     // HTTP 프로토콜 불일치
            "javascript:alert(1)"                                                  // XSS 공격 구문
    })
    @DisplayName("비정상 스포티파이 이미지 URL은 유효성 검증에서 실패하고 오류 메시지를 반환한다")
    void invalidSpotifyImage_FailsValidation(String invalidUrl) {
        // given
        PlaylistSongCreateRequest request = createRequestWithAlbumArt(invalidUrl);

        // when
        Set<ConstraintViolation<PlaylistSongCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("스포티파이 공식 이미지 URL 형식이어야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("곡 제목이 공백이나 띄어쓰기인 경우 @NotBlank 검증에 걸려 실패한다")
    void blankTitle_FailsValidation(String blankTitle) {
        // given
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "4cOdK2wGLETKBW3PvgPWqT",
                blankTitle,
                "NewJeans",
                "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290",
                "코멘트",
                UUID.randomUUID(),
                Set.of(Genre.KPOP)
        );

        // when
        Set<ConstraintViolation<PlaylistSongCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("곡 제목은 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("아티스트명이 공백이나 띄어쓰기인 경우 @NotBlank 검증에 걸려 실패한다")
    void blankArtist_FailsValidation(String blankArtist) {
        // given
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "4cOdK2wGLETKBW3PvgPWqT",
                "Ditto",
                blankArtist,
                "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290",
                "코멘트",
                UUID.randomUUID(),
                Set.of(Genre.KPOP)
        );

        // when
        Set<ConstraintViolation<PlaylistSongCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("아티스트명은 필수입니다.");
    }
}
