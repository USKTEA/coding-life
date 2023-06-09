package project.structure.sample

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Lob
import project.structure.model.BaseEntity
import project.structure.sample.view.SampleMessage

@Entity
class Sample(
    @Lob
    @Column(columnDefinition = "TEXT")
    var commonMessage: String? = null,

    @Column(columnDefinition = "TEXT")
    var adminMessage: String? = null,

    @Column(columnDefinition = "TEXT")
    var appMessage: String? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    var rawContent: String? = null
) : BaseEntity()

fun Sample.renderMessage() = SampleMessage(
    common = commonMessage,
    admin = adminMessage,
    app = appMessage
)
