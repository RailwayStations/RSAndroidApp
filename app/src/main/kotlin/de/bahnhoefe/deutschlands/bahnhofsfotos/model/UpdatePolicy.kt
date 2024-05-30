package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import de.bahnhoefe.deutschlands.bahnhofsfotos.R

enum class UpdatePolicy(val id: Int) {
    MANUAL(R.id.rb_update_manual),
    NOTIFY(R.id.rb_update_notify),
    AUTOMATIC(R.id.rb_update_automatic);
}

fun String?.toUpdatePolicy() = UpdatePolicy.entries
    .firstOrNull { it.toString() == this }
    ?: UpdatePolicy.NOTIFY
