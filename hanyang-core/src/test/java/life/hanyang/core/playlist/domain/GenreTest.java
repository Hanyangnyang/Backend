package life.hanyang.core.playlist.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenreTest {

    @Test
    void supportsEdmGenre() {
        assertThat(Genre.valueOf("EDM").getDescription()).isEqualTo("EDM");
    }

    @Test
    void supportsOstGenre() {
        assertThat(Genre.OST.getDescription()).isEqualTo("OST");
    }
}
