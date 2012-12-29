// Copyright Samuel Halliday 2012
package org.openyou.jpa;

import fommil.persistence.CrudDao;

import javax.persistence.EntityManagerFactory;
import java.util.UUID;

/**
 * @author Sam Halliday
 */
public class EmotivDatumCrud extends CrudDao<UUID, EmotivDatum> {

    public EmotivDatumCrud(EntityManagerFactory emf) {
        super(EmotivDatum.class, emf);
    }

}
