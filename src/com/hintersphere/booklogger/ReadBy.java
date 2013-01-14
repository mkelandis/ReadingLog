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
    
    private static final int POSITION_NOT_FOUND = -1;
    public final static ReadBy[] getById = {ReadBy.CHILD, ReadBy.PARENT, ReadBy.CHILD_PARENT, ReadBy.ME};
    public final static ReadBy[] displayPositions = {ReadBy.ME, ReadBy.CHILD, ReadBy.PARENT, ReadBy.CHILD_PARENT};
    public final short id;
    
    ReadBy(int id) {
        this.id = (short) id;
    }
    
    /**
     * Find the display position of a ReadBy selection.
     * @param id of ReadBy to get the position for
     * @return display position
     */
    static int getDisplayPosition(int id) {
        ReadBy readBy = getById[id];
        for (int i=0; i < displayPositions.length; i++) {
            if (displayPositions[i] == readBy) {
                return i;
            }
        }
        return POSITION_NOT_FOUND;
    }
    
}
