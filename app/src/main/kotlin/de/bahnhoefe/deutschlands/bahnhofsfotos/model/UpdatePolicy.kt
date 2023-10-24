package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import java.util.Arrays

enum class UpdatePolicy(val id: Int) {
    MANUAL(R.id.rb_update_manual),
    NOTIFY(R.id.rb_update_notify),
    AUTOMATIC(R.id.rb_update_automatic);

    companion object {

        @JvmStatic
        fun byName(name: String): UpdatePolicy {
            return Arrays.stream(values())
                .filter { updatePolicy: UpdatePolicy -> updatePolicy.toString() == name }
                .findFirst()
                .orElse(NOTIFY)
        }
    }
}