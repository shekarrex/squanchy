package net.squanchy.service.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.squanchy.eventdetails.domain.view.ExperienceLevel;
import net.squanchy.schedule.domain.view.Event;
import net.squanchy.service.firebase.FirebaseDbService;
import net.squanchy.service.firebase.model.FirebaseEvent;
import net.squanchy.service.firebase.model.FirebaseEvents;
import net.squanchy.service.firebase.model.FirebaseFavorites;
import net.squanchy.speaker.domain.view.Speaker;
import net.squanchy.support.lang.Checksum;
import net.squanchy.support.lang.Func1;
import net.squanchy.support.lang.Lists;
import net.squanchy.support.lang.Optional;

import org.joda.time.LocalDateTime;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;

import static net.squanchy.support.lang.Lists.filter;

public class EventRepository {

    private final FirebaseDbService dbService;
    private final Checksum checksum;
    private final SpeakerRepository speakerRepository;

    public EventRepository(FirebaseDbService dbService, Checksum checksum, SpeakerRepository speakerRepository) {
        this.dbService = dbService;
        this.checksum = checksum;
        this.speakerRepository = speakerRepository;
    }

    public Observable<Event> event(String eventId, String userId) {
        Observable<FirebaseEvent> eventObservable = dbService.event(eventId);
        Observable<List<Speaker>> speakersObservable = speakerRepository.speakers();
        Observable<FirebaseFavorites> favoritesObservable = dbService.favorites(userId);

        return Observable.combineLatest(
                eventObservable,
                speakersObservable,
                favoritesObservable,
                combineIntoEvent()
        ).subscribeOn(Schedulers.io());
    }

    private Function3<FirebaseEvent, List<Speaker>, FirebaseFavorites, Event> combineIntoEvent() {
        return (apiEvent, speakers, favorites) -> Event.create(
                apiEvent.id,
                checksum.getChecksumOf(apiEvent.id),
                apiEvent.day_id,
                new LocalDateTime(apiEvent.start_time),
                new LocalDateTime(apiEvent.end_time),
                apiEvent.name,
                apiEvent.place_id,
                Optional.fromNullable(apiEvent.experience_level).flatMap(ExperienceLevel::fromNullableRawLevel),
                speakersForEvent(apiEvent, speakers),
                Event.Type.fromRawType(apiEvent.type),
                favorites.hasFavorite(apiEvent.id)
        );
    }

    public Observable<List<Event>> events(String userId) {
        Observable<FirebaseEvents> sessionsObservable = dbService.events();
        Observable<List<Speaker>> speakersObservable = speakerRepository.speakers();
        Observable<FirebaseFavorites> favoritesObservable = dbService.favorites(userId);

        return Observable.combineLatest(sessionsObservable, speakersObservable, favoritesObservable, combineSessionsAndSpeakers());
    }

    private Function3<FirebaseEvents, List<Speaker>, FirebaseFavorites, List<Event>> combineSessionsAndSpeakers() {
        return (apiSchedule, speakers, favorites) -> Lists.map(new ArrayList<>(apiSchedule.events.values()), combineEventWith(speakers, favorites));
    }

    private Func1<FirebaseEvent, Event> combineEventWith(List<Speaker> speakers, FirebaseFavorites favorites) {
        return apiEvent -> Event.create(
                apiEvent.id,
                checksum.getChecksumOf(apiEvent.id),
                apiEvent.day_id,
                new LocalDateTime(apiEvent.start_time),
                new LocalDateTime(apiEvent.end_time),
                apiEvent.name,
                apiEvent.place_id,
                Optional.fromNullable(apiEvent.experience_level).flatMap(ExperienceLevel::fromNullableRawLevel),
                speakersForEvent(apiEvent, speakers),
                Event.Type.fromRawType(apiEvent.type),
                favorites.hasFavorite(apiEvent.id)
        );
    }

    private List<Speaker> speakersForEvent(FirebaseEvent apiEvent, List<Speaker> speakers) {
        if (apiEvent.speaker_ids == null || apiEvent.speaker_ids.isEmpty()) {
            return Collections.emptyList();
        }

        return filter(speakers, speaker -> apiEvent.speaker_ids.contains(speaker.id()));
    }

    public Completable addFavorite(String eventId, String userId) {
        return dbService.addFavorite(eventId, userId);
    }

    public Completable removeFavorite(String eventId, String userId) {
        return dbService.removeFavorite(eventId, userId);
    }
}
