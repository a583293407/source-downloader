package xyz.shoaky.sourcedownloader.repo.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "target_path_record")
class TargetPathRecord {
    @Id
    lateinit var id: String
    var processingId: Long? = null
    lateinit var createTime: LocalDateTime
}