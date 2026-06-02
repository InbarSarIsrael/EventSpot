package com.eventspot.app.utilities

import com.eventspot.app.model.Event

class RecommendationEngine {

    fun getRecommendedEvents(events: List<Event>, preferredCategories: List<String>,
        savedEvents: List<Event> = emptyList(), currentTimeMillis: Long = System.currentTimeMillis(),
        limit: Int = 5): List<Event> {
        val savedEventIds = savedEvents.map { it.id }.toSet()
        val savedCategories = savedEvents.flatMap { it.categories }

        val candidateEvents = events
            .filterNot { it.id in savedEventIds }
            .filter { event -> getEventEndMillis(event) >= currentTimeMillis }

        if (savedCategories.isEmpty()) {
            return candidateEvents
                .sortedBy { it.dateTimeMillis }
                .take(limit)
        }

        return candidateEvents
            .map { event ->
                val preferredCategoryScore = event.categories.count { eventCategory ->
                    preferredCategories.any { it.equals(eventCategory, ignoreCase = true) }
                } * 3

                val savedCategoryScore = event.categories.count { eventCategory ->
                    savedCategories.any { it.equals(eventCategory, ignoreCase = true) }
                } * 5

                val interestScore = preferredCategoryScore + savedCategoryScore
                val timeScore = if (interestScore > 0) {
                    calculateTimeScore(event, currentTimeMillis)
                } else {
                    0
                }

                event to (interestScore + timeScore)
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (event, _) -> event }
            .take(limit)
    }

    private fun calculateTimeScore(event: Event, currentTimeMillis: Long): Int {
        val millisUntilEvent = event.dateTimeMillis - currentTimeMillis

        if (millisUntilEvent <= WEEK_IN_MILLIS) {
            return HIGH_TIME_SCORE
        } else if (millisUntilEvent <= MONTH_IN_MILLIS) {
            return MEDIUM_TIME_SCORE
        } else {
            return LOW_TIME_SCORE
        }
    }

    private fun getEventEndMillis(event: Event): Long {
        return if (event.endTimeMillis > 0L) {
            event.endTimeMillis
        } else {
            event.dateTimeMillis
        }
    }

    private companion object {
        const val WEEK_IN_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val MONTH_IN_MILLIS = 30L * 24 * 60 * 60 * 1000

        const val HIGH_TIME_SCORE = 3
        const val MEDIUM_TIME_SCORE = 2
        const val LOW_TIME_SCORE = 1
    }
}