package sparse.bean;

import javafx.util.Pair;

import java.util.Objects;

public class TripleID
{
    public Pair<Integer,Integer> sub;
    public Pair<Integer,Integer> pre;
    public Pair<Integer,Integer> obj;

    public TripleID(Pair<Integer, Integer> sub, Pair<Integer, Integer> pre, Pair<Integer, Integer> obj) {
        this.sub = sub;
        this.pre = pre;
        this.obj = obj;
    }

    public Pair<Integer, Integer> getSub() {
        return sub;
    }

    public void setSub(Pair<Integer, Integer> sub) {
        this.sub = sub;
    }

    public Pair<Integer, Integer> getPre() {
        return pre;
    }

    public void setPre(Pair<Integer, Integer> pre) {
        this.pre = pre;
    }

    public Pair<Integer, Integer> getObj() {
        return obj;
    }

    public void setObj(Pair<Integer, Integer> obj) {
        this.obj = obj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TripleID)) return false;
        TripleID tripleID = (TripleID) o;
        return getSub().equals(tripleID.getSub()) && getPre().equals(tripleID.getPre()) && getObj().equals(tripleID.getObj());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSub(), getPre(), getObj());
    }
}