package com.chaos.tablist.client.editor;

import java.util.concurrent.atomic.AtomicLong;
import com.chaos.tablist.net.TabPayloads;

/** Draft persistence state, separate from widgets and tested without a running client. */
public final class EditorSaveState {
    public record Pending(long id, long revision, String json, long startedAt) {}
    private static final AtomicLong IDS = new AtomicLong();
    private long revision;
    private String savedJson;
    private Pending pending;
    private String error = "";

    public EditorSaveState(long revision, String json) { this.revision = revision; savedJson = json; }
    public long revision() { return revision; }
    public String savedJson() { return savedJson; }
    public Pending pending() { return pending; }
    public String error() { return error; }

    public Pending begin(String json, long now) {
        if (pending != null) throw new IllegalStateException("A save is already pending");
        error = "";
        pending = new Pending(IDS.incrementAndGet(), revision, json, now);
        return pending;
    }

    public boolean accept(TabPayloads.SaveResult result) {
        if (pending == null || pending.id() != result.requestId()) return false;
        if (result.success()) { savedJson = pending.json(); revision = result.revision(); error = ""; }
        else error = result.reason();
        pending = null;
        return true;
    }

    public void abort(String reason) { pending = null; error = reason; }

    public boolean timedOut(long now) {
        if (pending == null || now - pending.startedAt() < 15000) return false;
        pending = null; error = "timeout";
        return true;
    }
}
