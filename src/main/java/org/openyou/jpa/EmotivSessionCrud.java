// Copyright Samuel Halliday 2012
package org.openyou.jpa;

import fommil.persistence.CrudDao;

import javax.persistence.EntityManagerFactory;
import java.util.UUID;

/**
 * @author Sam Halliday
 */
public class EmotivSessionCrud extends CrudDao<UUID, EmotivSession> {

    public EmotivSessionCrud(EntityManagerFactory emf) {
        super(EmotivSession.class, emf);
    }

}
