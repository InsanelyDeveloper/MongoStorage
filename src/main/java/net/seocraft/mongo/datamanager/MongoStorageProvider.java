package net.seocraft.mongo.datamanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.mongodb.DB;
import net.seocraft.mongo.concurrent.AsyncResponse;
import net.seocraft.mongo.concurrent.SimpleAsyncResponse;
import net.seocraft.mongo.concurrent.WrappedResponse;
import net.seocraft.mongo.models.Model;
import org.jetbrains.annotations.NotNull;
import org.mongojack.JacksonDBCollection;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MongoStorageProvider<T extends Model> implements StorageProvider<T> {

    private ListeningExecutorService executorService;
    private JacksonDBCollection<T, String> mongoCollection;

    public MongoStorageProvider(@NotNull ListeningExecutorService executorService, @NotNull DB database, @NotNull String dataPrefix, @NotNull Class<T> modelClass, @NotNull ObjectMapper mapper) {
        this.executorService = executorService;
        this.mongoCollection = JacksonDBCollection.wrap(database.getCollection(dataPrefix),
                modelClass,
                String.class,
                mapper
        );
    }

    @Override
    public @NotNull AsyncResponse<T> findOne(@NotNull String id) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> {
            Optional<T> response = this.findOneSync(id);
            return response.map(model -> new WrappedResponse<>(WrappedResponse.Status.SUCCESS, model, null))
                    .orElseGet(() -> new WrappedResponse<>(WrappedResponse.Status.ERROR, null, null));
        }));
    }

    @Override
    public @NotNull AsyncResponse<Set<T>> find(@NotNull Set<String> ids) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> new WrappedResponse<>(WrappedResponse.Status.SUCCESS, this.findSync(ids), null)));
    }

    @Override
    public @NotNull AsyncResponse<Set<T>> find(int limit) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> new WrappedResponse<>( WrappedResponse.Status.SUCCESS, this.findSync(limit), null)));
    }

    @Override
    public @NotNull AsyncResponse<Set<T>> find() {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> new WrappedResponse<>(WrappedResponse.Status.SUCCESS, this.findSync(), null)));
    }

    @Override
    public AsyncResponse<Void> save(@NotNull T object) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> {
            this.saveSync(object);
            return null;
        }));
    }

    @Override
    public AsyncResponse<Void> save(@NotNull Set<T> objects) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> {
            this.saveSync(objects);
            return null;
        }));
    }

    @Override
    public AsyncResponse<Void> delete(@NotNull String id) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> {
            this.deleteSync(id);
            return null;
        }));
    }

    @Override
    public AsyncResponse<Void> delete(@NotNull T object) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> {
            this.deleteSync(object);
            return null;
        }));
    }

    @Override
    public AsyncResponse<Void> delete(@NotNull Set<T> objects) {
        return new SimpleAsyncResponse<>(this.executorService.submit(() -> {
            this.deleteSync(objects);
            return null;
        }));
    }

    @Override
    public Optional<T> findOneSync(@NotNull String id) {
        return Optional.ofNullable(this.mongoCollection.findOneById(id));
    }

    @Override
    public @NotNull Set<T> findSync(@NotNull Set<String> ids) {
        Set<T> objects = new HashSet<>();
        ids.forEach(id -> this.findOneSync(id).ifPresent(objects::add));
        return objects;
    }

    @Override
    public @NotNull Set<T> findSync(int limit) {
        return new HashSet<>(this.mongoCollection.find().limit(limit).toArray());
    }

    @Override
    public @NotNull Set<T> findSync() {
        return new HashSet<>(this.mongoCollection.find().toArray());
    }

    @Override
    public void saveSync(@NotNull T object) {
        this.mongoCollection.save(object);
    }

    @Override
    public void saveSync(@NotNull Set<T> objects) {
        objects.forEach(this::saveSync);
    }

    @Override
    public void deleteSync(@NotNull String id) {
        this.mongoCollection.removeById(id);
    }

    @Override
    public void deleteSync(@NotNull T object) {
        this.deleteSync(object.getId());
    }

    @Override
    public void deleteSync(@NotNull Set<T> objects) {
        objects.forEach(this::deleteSync);
    }
}