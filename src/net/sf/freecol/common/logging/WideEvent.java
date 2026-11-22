/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * Accumulates the facts about one bounded unit of work (e.g. a single
 * AI player's turn) into a single structured log record, instead of
 * many independent, uncorrelated log lines.
 *
 * Fields are rendered as {@code key=value} pairs in insertion order.
 * Repeated {@link #note} calls under the same key are collected into
 * a list, so several noteworthy occurrences during the unit of work
 * still result in exactly one emitted record.
 */
public final class WideEvent {

    /**
     * Per-thread stack of events currently in progress, innermost on
     * top.  Lets code that has no direct reference to the enclosing
     * event (e.g. a model class invoked deep under a controller
     * method) still contribute a {@link #note} to it via the static
     * helpers below, without threading the event through every
     * intervening method signature.
     */
    private static final ThreadLocal<Deque<WideEvent>> CONTEXT
        = ThreadLocal.withInitial(ArrayDeque::new);

    /** Used to recover the true caller for {@link #noteOrLog}'s fallback. */
    private static final StackWalker CALLER_WALKER = StackWalker.getInstance();

    /**
     * When {@code true}, {@link #emit} renders events as JSON instead
     * of the default {@code key=value} text.  Opt in with
     * {@code -Dfreecol.wideEvents.json=true}; defaults to off, so
     * existing behaviour and log formats are unchanged unless a
     * deployment explicitly asks for structured output.
     */
    private static final boolean JSON_ENABLED
        = Boolean.getBoolean("freecol.wideEvents.json");

    private final String name;
    private final long startNanos;
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private Throwable failure;


    /**
     * Start a new wide event and make it the current one for this
     * thread, so that {@link #note(String, Object)} reaches it from
     * anywhere on the call stack until {@link #end} is called.
     *
     * @param name A short, stable identifier for the kind of event.
     * @return The new, now-current event.
     */
    public static WideEvent begin(String name) {
        WideEvent event = new WideEvent(name);
        CONTEXT.get().push(event);
        return event;
    }

    /**
     * Record a noteworthy occurrence on the innermost event currently
     * in progress on this thread, if any.  A no-op when no event is
     * active, so callers do not need to guard this themselves.
     *
     * @param category The category to group this note under.
     * @param detail The detail to record.
     */
    public static void note(String category, Object detail) {
        WideEvent event = CONTEXT.get().peek();
        if (event != null) event.note0(category, detail);
    }

    /**
     * Record a noteworthy occurrence on the current event if one is
     * active, otherwise fall back to an ordinary independent log
     * line.  This is the usual way call sites deep in the model or
     * GUI should report something worth keeping: it stays correlated
     * with whatever bounded operation triggered it when one is in
     * progress, and degrades gracefully to plain logging otherwise
     * (e.g. when invoked from a path with no wide event boundary
     * yet, such as end-of-turn housekeeping).
     *
     * @param logger The {@code Logger} to fall back to.
     * @param level The level to fall back to logging at.
     * @param category The category to group the note under.
     * @param detail The detail to record.
     */
    public static void noteOrLog(Logger logger, Level level,
                                  String category, Object detail) {
        WideEvent event = CONTEXT.get().peek();
        if (event != null) {
            event.note0(category, detail);
        } else if (logger.isLoggable(level)) {
            // Build the record explicitly and set its source class/method
            // from the true caller.  Routing through this helper adds a
            // stack frame that would otherwise fool java.util.logging's
            // caller inference into blaming noteOrLog itself.
            StackWalker.StackFrame caller = CALLER_WALKER.walk(frames -> frames
                .filter(f -> !f.getClassName().equals(WideEvent.class.getName()))
                .findFirst())
                .orElse(null);
            LogRecord record = new LogRecord(level, String.valueOf(detail));
            record.setLoggerName(logger.getName());
            if (caller != null) {
                record.setSourceClassName(caller.getClassName());
                record.setSourceMethodName(caller.getMethodName());
            }
            logger.log(record);
        }
    }

    /**
     * Finish this event: emit it, then pop it from the thread's
     * context stack so an enclosing event (if any) becomes current
     * again.
     *
     * @param logger The {@code Logger} to emit to.
     * @param level The level to emit at.
     */
    public void end(Logger logger, Level level) {
        Deque<WideEvent> stack = CONTEXT.get();
        if (stack.peek() == this) {
            stack.pop();
        } else {
            stack.remove(this);
        }
        emit(logger, level);
    }

    /**
     * Start timing a new wide event.
     *
     * @param name A short, stable identifier for the kind of event,
     *     e.g. {@code "ai.european.turn"}.
     */
    public WideEvent(String name) {
        this.name = name;
        this.startNanos = System.nanoTime();
    }

    /**
     * Attach a single field to the event, overwriting any previous
     * value under the same key.
     *
     * @param key The field name.
     * @param value The field value.
     * @return This event, for chaining.
     */
    public WideEvent with(String key, Object value) {
        fields.put(key, value);
        return this;
    }

    /**
     * Record a noteworthy occurrence under a category, without
     * emitting a separate log line for it.  Multiple notes under the
     * same category accumulate into a list.
     *
     * @param category The category to group this note under,
     *     e.g. {@code "warning"}.
     * @param detail The detail to record.
     * @return This event, for chaining.
     */
    public WideEvent addNote(String category, Object detail) {
        note0(category, detail);
        return this;
    }

    @SuppressWarnings("unchecked")
    private void note0(String category, Object detail) {
        List<Object> notes = (List<Object>)fields
            .computeIfAbsent(category, k -> new ArrayList<>());
        notes.add(detail);
    }

    /**
     * Mark the event as having failed with the given exception.  The
     * exception is attached to the emitted record as its thrown
     * cause rather than as a formatted field.
     *
     * @param t The {@code Throwable} responsible for the failure.
     * @return This event, for chaining.
     */
    public WideEvent fail(Throwable t) {
        this.failure = t;
        return this;
    }

    /**
     * Emit the accumulated event as a single log record, if the
     * given level is enabled.
     *
     * @param logger The {@code Logger} to emit to.
     * @param level The level to emit at.
     */
    public void emit(Logger logger, Level level) {
        if (!logger.isLoggable(level)) return;

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        String message = JSON_ENABLED ? toJson(durationMs) : toText(durationMs);

        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(logger.getName());
        if (failure != null) record.setThrown(failure);
        logger.log(record);
    }

    /**
     * Render this event in the default {@code key=value} text form.
     *
     * @param durationMs The event's duration in milliseconds.
     * @return The rendered line.
     */
    private String toText(long durationMs) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("event=").append(name).append(" duration_ms=").append(durationMs);
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            sb.append(' ').append(e.getKey()).append('=')
                .append(format(e.getValue()));
        }
        return sb.toString();
    }

    /**
     * Format a field value for inclusion in a text-rendered line,
     * quoting it if it contains whitespace.
     *
     * @param value The value to format.
     * @return The formatted value.
     */
    private static String format(Object value) {
        String s = String.valueOf(value);
        if (s.indexOf(' ') < 0) return s;
        return '"' + s.replace("\"", "\\\"") + '"';
    }

    /**
     * Render this event as a single JSON object.  Enabled by setting
     * the {@code freecol.wideEvents.json} system property to
     * {@code true}; otherwise {@link #toText} is used.  This is an
     * output-format choice only: field names, the event name, and
     * fallback behaviour are unchanged either way, so no call site
     * needs to know or care which is active.
     *
     * @param durationMs The event's duration in milliseconds.
     * @return The rendered JSON object, as a single line.
     */
    private String toJson(long durationMs) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"event\":").append(jsonString(name))
            .append(",\"duration_ms\":").append(durationMs);
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            sb.append(',').append(jsonString(e.getKey())).append(':')
                .append(jsonValue(e.getValue()));
        }
        return sb.append('}').toString();
    }

    /**
     * Render a single field value as a JSON value.
     *
     * @param value The value to render.
     * @return The rendered JSON value.
     */
    private static String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?>) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : (List<?>)value) {
                if (!first) sb.append(',');
                first = false;
                sb.append(jsonValue(o));
            }
            return sb.append(']').toString();
        }
        return jsonString(String.valueOf(value));
    }

    /**
     * Escape and quote a string for inclusion in JSON output.
     *
     * @param s The string to escape.
     * @return The escaped, quoted string.
     */
    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '"':  sb.append("\\\""); break;
            case '\\': sb.append("\\\\"); break;
            case '\n': sb.append("\\n"); break;
            case '\r': sb.append("\\r"); break;
            case '\t': sb.append("\\t"); break;
            default:
                if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int)c));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
