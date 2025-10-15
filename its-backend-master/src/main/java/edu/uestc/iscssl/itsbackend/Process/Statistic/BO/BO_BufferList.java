package edu.uestc.iscssl.itsbackend.Process.Statistic.BO;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class BO_BufferList<E> extends LinkedList<E>{

    transient private final int BUFFER_CAPASITY;

    public BO_BufferList(int capasity){
        super();
        this.BUFFER_CAPASITY = capasity;
    }

    public BO_BufferList(int capasity, Collection<E> e){
        this(capasity);
        this.addAll(e);
    }

    public int getBufferCapasity(){
        return BUFFER_CAPASITY;
    }

    @Override
    public boolean add(E e) {
        if(this.size()>(BUFFER_CAPASITY-1))this.remove(0);
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            modified = add(e);
        return modified;
    }

    @Override
    public Object clone() {
        return new BO_BufferList<E>(BUFFER_CAPASITY,this);
    }

    /**
     * 以下过程禁用
     * */

    @Deprecated
    @Override
    public boolean addAll(int index,Collection<? extends E> c) {
        return false;
    }

    @Deprecated
    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Deprecated
    @Override
    public E set(int index, E element) {
        return null;
    }

    @Deprecated
    @Override
    public void add(int index, E element) {

    }



}