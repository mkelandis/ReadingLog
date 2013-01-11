package com.hintersphere.booklogger;

/**
 * Stores available read by activities, their id and their display position.
 * @author mlandis
 */
public enum ReadBy {
    
    CHILD(0),
    PARENT(1),
    CHILD_PARENT(2),
    ME(3);
    
    public final static ReadBy[] getById = {ReadBy.CHILD, ReadBy.PARENT, ReadBy.CHILD_PARENT, ReadBy.ME};
    public final static ReadBy[] displayPositions = {ReadBy.ME, ReadBy.CHILD, ReadBy.PARENT, ReadBy.CHILD_PARENT};
    public final short id;
    
    ReadBy(int id) {
        this.id = (short) id;
    }
    
}
