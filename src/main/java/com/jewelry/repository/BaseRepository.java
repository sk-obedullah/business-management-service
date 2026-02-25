package com.jewelry.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract for all repositories.
 *
 * <p>
 * Follows the Interface Segregation Principle — repositories that need only
 * read operations can extend a read-only sub-interface rather than this full
 * interface. For Phase 1 all concrete repos implement the full contract.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> replace with
 * {@code JpaRepository<T, ID>} from Spring Data. Method signatures are
 * intentionally
 * aligned with Spring Data conventions ({@code findById}, {@code findAll},
 * {@code save}, {@code deleteById}) to minimise migration refactoring.
 *
 * @param <T>  entity type
 * @param <ID> primary-key type
 */
public interface BaseRepository<T, ID> {

    /**
     * Persists a new entity and returns it with its generated primary key
     * populated.
     *
     * @param entity the entity to persist (id should be null / 0)
     * @return the persisted entity with generated id
     */
    T save(T entity);

    /**
     * Finds an entity by its primary key.
     *
     * @param id primary key
     * @return {@link Optional#of(Object)} if present, {@link Optional#empty()}
     *         otherwise
     */
    Optional<T> findById(ID id);

    /**
     * Returns all entities of this type ordered by natural insertion order.
     *
     * @return mutable list; never {@code null}
     */
    List<T> findAll();

    /**
     * Updates all mutable fields of an existing entity.
     *
     * @param entity entity with an existing id and updated fields
     */
    void update(T entity);

    /**
     * Deletes the entity with the given primary key.
     *
     * @param id primary key; silently ignored if entity does not exist
     */
    void deleteById(ID id);
}
