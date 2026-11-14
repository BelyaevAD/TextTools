package dev.belyaevad.texttools.domain

const val HISTORY_LIMIT: Int = 20
const val TEXT_LIMIT: Int = 8000

/** Trims stored source and result text. */
fun trimStored(text: String): String = if (text.length <= TEXT_LIMIT) text else text.substring(0, TEXT_LIMIT)

/**
 * Keeps the system message and a user-led tail.
 * Nine or fewer messages are returned unchanged. Otherwise the tail starts at length-7,
 * moved forward by one when that message is not a user turn.
 */
fun trimConversation(messages: List<ChatMessage>): List<ChatMessage> {
    if (messages.size <= 9) {
        return messages
    }
    var start = messages.size - 7
    if (messages[start].role != "user") {
        start += 1
    }
    return listOf(messages.first()) + messages.subList(start, messages.size)
}
