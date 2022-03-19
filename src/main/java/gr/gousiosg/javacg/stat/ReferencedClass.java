package gr.gousiosg.javacg.stat;

import java.util.Objects;

public class ReferencedClass {
    private String called;
    private Integer referenceCount;

    public ReferencedClass(String called) {
        this.called = called;
        this.referenceCount = 1;
    }

    public String getCalled() {
        return called;
    }

    public void referenced() {
        this.referenceCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferencedClass that = (ReferencedClass) o;
        return Objects.equals(called, that.called);
    }

    @Override
    public int hashCode() {
        return Objects.hash(called);
    }
}
