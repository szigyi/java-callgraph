package gr.gousiosg.javacg.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Visited {
    private String caller;
    private Map<String, Integer> called;
    private Integer referencedByOthers;

    public Visited(String caller) {
        this.caller = rootClassName(caller);
        this.called = new HashMap<>();
        this.referencedByOthers = 0;
    }

    private String rootClassName(String c) {
        Pattern p = Pattern.compile("^.+?(?=\\$)");
        Matcher m = p.matcher(c);
        if (m.find()) {
            return m.group(0);
        } else {
            return c;
        }
    }

    public void referenced(String calledRaw) {
        String called = rootClassName(calledRaw);

        if (this.called.containsKey(called)) {
            Integer counter = this.called.get(called);
            this.called.put(called, counter++);
        } else {
            this.called.put(called, 1);
        }
    }

    public void setReferencedByOthers(Integer referencedByOthers) {
        this.referencedByOthers = referencedByOthers;
    }

    public Integer getReferencedByOthers() {
        return referencedByOthers;
    }

    public String getCaller() {
        return caller;
    }

    public Map<String, Integer> getCalled() {
        return called;
    }
}
