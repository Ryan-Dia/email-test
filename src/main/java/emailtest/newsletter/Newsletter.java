package emailtest.newsletter;

import emailtest.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Newsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 512)
    private String imageUrl;

    @Column(nullable = false, length = 30)
    private String email;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private Long detailId;

    @Builder
    public Newsletter(
            Long id,
            @NonNull String name,
            @NonNull String description,
            @NonNull String imageUrl,
            @NonNull String email,
            @NonNull Long categoryId,
            @NonNull Long detailId
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.email = email;
        this.categoryId = categoryId;
        this.detailId = detailId;
    }
}
