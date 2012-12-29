// Copyright Samuel Halliday 2009
package fommil.persistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import lombok.Cleanup;
import lombok.extern.java.Log;

import javax.persistence.*;
import java.io.File;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Generic CRUD (CREATE, READ, UPDATE, DELETE) DAO (Data Access Object) for {@link Entity} types.
 * This abstract class is designed to be extended by DAOs in order to provide basic functionality
 * and reduce boilerplate.
 * <p/>
 * <b>WARNING</b>: each of these methods is contained in a single {@link EntityTransaction}, which means that
 * clients will be prone to Last Commit Wins concurrency problems that can result in data loss.
 * <p/>
 * Implementations are reminded that many convenience methods are provided with {@code protected}
 * visibility.
 *
 * @param <K> the class of the type's primary key
 * @param <T> the class of the type
 * @author Samuel Halliday
 * @see <a href="http://code.google.com/p/hibernate-generic-dao/">Hibernate Generic DAO</a>
 * @see <a href="http://code.google.com/p/generic-dao/">Generic DAO</a>
 */
@Log
public abstract class CrudDao<K, T> {

    private static final String JPA_PROPERTIES = "jpa.properties";

    /**
     * Convenience method for creating an {@link EntityManagerFactory}. This has several advantages
     * over using {@link Persistence#createEntityManagerFactory(String)}:
     * <ul>
     * <li>throws {@link ExceptionInInitializerError} which makes it safe to assign to
     * a static field</li>
     * <li>if a {@value #JPA_PROPERTIES} file is found, the contents will override the
     * {@code persistence.xml} file and the {@code (non-)jta-data-source} values will be reset.
     * This essentially lets the {@value #JPA_PROPERTIES} define J2SE behaviour, whilst
     * the {@code persistence.xml} file is used in J2EE deployment.</li>
     * </ul>
     * Clients are expected to close the {@link EntityManagerFactory} when it is no longer required,
     * failure to do so could result in side effects such as data loss or incomplete table strategies.
     *
     * @param persistenceUnit
     * @return
     * @throws ExceptionInInitializerError
     */
    public static EntityManagerFactory createEntityManagerFactory(String persistenceUnit)
            throws ExceptionInInitializerError {
        try {
            Preconditions.checkNotNull(persistenceUnit);
            File hibernateProps = new File(JPA_PROPERTIES);
            if (hibernateProps.exists()) {
                Properties properties = new Properties();
                @Cleanup Reader reader = Files.newReader(hibernateProps, Charset.defaultCharset());
                properties.load(reader);
                properties.put("javax.persistence.jtaDataSource", "");
                properties.put("javax.persistence.nonJtaDataSource", "");
                log.info(properties.toString());
                return Persistence.createEntityManagerFactory(persistenceUnit, properties);
            }
            return Persistence.createEntityManagerFactory(persistenceUnit);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Class<T> klass;

    private final EntityManagerFactory emf;

    /**
     * @param klass class of the entity type, must be an {@link Entity} type.
     * @param emf
     */
    protected CrudDao(Class<T> klass, EntityManagerFactory emf) {
        this.klass = Preconditions.checkNotNull(klass);
        this.emf = Preconditions.checkNotNull(emf);
        Preconditions.checkArgument(klass.getAnnotations() != null);
        boolean entity = false;
        for (Annotation annotation : klass.getAnnotations()) {
            if (annotation.annotationType().equals(Entity.class)) {
                entity = true;
            }
        }
        Preconditions.checkArgument(entity);
    }

    /**
     * @return an entity manager, must be closed after use.
     */
    protected EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Note that if the primary key is user-generated, you will experience an exception if an
     * entity already exists with that key.
     *
     * @param entity
     * @throws PersistenceException
     */
    public void create(T entity) {
        Preconditions.checkNotNull(entity);
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Note that if the primary key is user-generated, you will experience an exception if an
     * entity already exists with the key of any of these entities and the entire create call
     * will fail.
     *
     * @param collection
     * @throws PersistenceException
     */
    public void create(Collection<T> collection) {
        Preconditions.checkNotNull(collection);
        if (collection.isEmpty()) {
            return;
        }
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            for (T entity : collection) {
                em.persist(entity);
            }
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * @param key
     * @return the entity with the given key, or null if not found
     * @throws PersistenceException
     */
    public T read(K key) {
        Preconditions.checkNotNull(key);
        @Cleanup("close") EntityManager em = createEntityManager();
        return em.find(klass, key);
    }

    /**
     * @param keys
     * @return the entities with the given keys, or null if not found
     * @throws PersistenceException
     */
    public List<T> read(Collection<K> keys) {
        Preconditions.checkNotNull(keys);
        List<T> results = Lists.newArrayList();
        @Cleanup("close") EntityManager em = createEntityManager();
        for (K key : keys) {
            T result = em.find(klass, key);
            results.add(result);
        }
        return results;
    }

    /**
     * Update the database with the existing entity.
     *
     * @param entity
     * @return the updated entity (which will be different from the one passed in)
     * @throws PersistenceException
     */
    @SuppressWarnings("AssignmentToMethodParameter")
    public T update(T entity) {
        Preconditions.checkNotNull(entity);
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            entity = em.merge(entity);
            em.getTransaction().commit();
            return entity;
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Update the database with existing entities.
     *
     * @param entities
     * @return
     * @throws PersistenceException
     */
    @SuppressWarnings("AssignmentToForLoopParameter")
    public Set<T> update(Collection<T> entities) {
        Preconditions.checkNotNull(entities);
        if (entities.isEmpty()) {
            return Collections.emptySet();
        }
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            Set<T> updated = Sets.newHashSet();
            for (T entity : entities) {
                entity = em.merge(entity);
                updated.add(entity);
            }
            em.getTransaction().commit();
            return updated;
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * @param entity
     * @throws PersistenceException
     */
    @SuppressWarnings("AssignmentToMethodParameter")
    public void delete(T entity) {
        Preconditions.checkNotNull(entity);
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            entity = em.merge(entity);
            removeFromManyToManyMappings(em, entity);
            em.remove(entity);
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * @param entities
     * @throws PersistenceException
     */
    @SuppressWarnings("AssignmentToForLoopParameter")
    public void delete(Collection<T> entities) {
        Preconditions.checkNotNull(entities);
        if (entities.isEmpty()) {
            return;
        }
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            for (T entity : entities) {
                entity = em.merge(entity);
                removeFromManyToManyMappings(em, entity);
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * @param id
     * @throws PersistenceException
     */
    public void deleteById(K id) {
        Preconditions.checkNotNull(id);
        @Cleanup("close") EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
//            T entity = em.find(klass, id);
            T entity = em.getReference(klass, id);
            if (entity == null) {
                em.getTransaction().rollback();
                return;
            }
            removeFromManyToManyMappings(em, entity);
            em.remove(entity);
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * @return a count of the number of entities in the database.
     */
    public long count() {
        EntityManager em = createEntityManager();
        Query q = em.createQuery("SELECT COUNT(s) FROM " + getTableName() + " s");
        Long result = querySingle(em, q);
        return result;
    }

    /**
     * Return all entities in the database.
     * <p/>
     * <b>WARNING: ONLY TO BE USED FOR SMALL DATABASES</b>
     *
     * @return
     */
    public List<T> readAll() {
        EntityManager em = createEntityManager();
        Query query = em.createQuery("SELECT s FROM " + getTableName() + " s");
        return query(em, query);
    }

    /**
     * @return the table name in JPA SQL (i.e. the simple class name of the managed entity)
     */
    protected String getTableName() {
        return klass.getSimpleName();
    }

    /**
     * Boilerplate saver - runs the given {@link Query}, expecting a list of {@link Entity}s of given
     * type. Never returns {@code null}.
     *
     * @param <C>
     * @param em
     * @param query
     * @return
     */
    protected <C> List<C> query(EntityManager em, Query query) {
        Preconditions.checkNotNull(em);
        Preconditions.checkNotNull(query);
        @SuppressWarnings("unchecked")
        List<C> result = (List<C>) query.getResultList();
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * Boilerplate saver - runs the given {@link Query}, expecting a single {@link Entity} of type
     * {@code T}. Never returns {@code null}.
     *
     * @param <C>
     * @param em
     * @param query
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <C> C querySingle(EntityManager em, Query query) {
        Preconditions.checkNotNull(em);
        Preconditions.checkNotNull(query);
        return (C) query.getSingleResult();
    }

    /**
     * Called within {@code DELETE} transactions to allow specialist DAOs
     * to remove foreign key references from other tables (i.e. if this entity
     * is the target of a {@link ManyToMany} collection in another entity). No
     * need to do anything if the entity is the "owner" of a bi-directional
     * relationship.
     * <p/>
     * This will be called from within a transaction. If the relationship is
     * bi-directional then the following pattern usually works:
     * <code><pre>
     * Collection<Other> others = entity.getOthers();
     * for (Other other : others) {
     *    other.getEntity().remove(entity);
     *    em.merge(other);
     * }
     * </pre></code>
     * whereas for uni-directional a JPQL query is needed, e.g.:
     * <code><pre>
     * Query q = em.createQuery("SELECT DISTINCT o FROM Other o INNER JOIN o.entity e where e.id = :id");
     * q.setParameter("id", entity.getId());
     *
     * @param em     which has already begun a transaction
     * @param entity managed
     * @SuppressWarnings("unchecked") Collection<Other> others = q.getResultList();
     * ...
     * </pre></code>
     * @see <a href="http://stackoverflow.com/questions/1980177">Cascade on delete using unidirectional Many-To-Many mapping</a>
     * @see <a href="http://stackoverflow.com/questions/1082095">How to remove entity with ManyToMany relationship in JPA</a>
     * @see <a href="http://stackoverflow.com/questions/8339889">JPA JPQL: select items when attribute of item (list/set) contains another item</a>
     */
    protected void removeFromManyToManyMappings(EntityManager em, T entity) {
    }
}
