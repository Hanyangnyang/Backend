package life.hanyang.core.menu.entity;

import jakarta.persistence.*;
import life.hanyang.core.menu.util.MenuParserUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "menu")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id", nullable = false)
    private Cafeteria cafeteria;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private MealType type;

    // 1. 웹사이트에서 긁어온 원본 그대로 (백업/비교용)
    @Column(columnDefinition = "TEXT")
    private String rawMenu;

    // 2. 실제 사용자(프론트엔드)에게 예쁘게 보여줄 정제된 메뉴 텍스트
    @Column(columnDefinition = "TEXT")
    private String displayMenu;

    private String price;

    private boolean isOverridden;

    public Menu(Cafeteria cafeteria, LocalDate date, MealType type, String rawText) {
        this.cafeteria = cafeteria;
        this.date = date;
        this.type = type;
        this.rawMenu = rawText;

        MenuParserUtils.ParsedMenu parsed = MenuParserUtils.cleanUpMenuText(rawText);
        this.displayMenu = parsed.cleanedMenu();
        this.price = parsed.price();
        this.isOverridden = false;
    }

    public Menu(Cafeteria cafeteria, LocalDate date, MealType type, String rawText, String displayMenu, String price) {
        this.cafeteria = cafeteria;
        this.date = date;
        this.type = type;
        this.rawMenu = rawText;
        this.displayMenu = displayMenu;
        this.price = price;
        this.isOverridden = false;
    }

    public void updateContent(String rawText, String displayMenu, String price) {
        if (!this.isOverridden) {
            this.rawMenu = rawText;
            this.displayMenu = displayMenu;
            this.price = price;
        }
    }

    public void updateDisplayMenu(String newDisplayMenu) {
        this.displayMenu = newDisplayMenu;
        this.isOverridden = true;
    }

    public boolean containsKeyword(String keyword) {
        if (this.displayMenu == null) return false;
        return this.displayMenu.contains(keyword);
    }
}
