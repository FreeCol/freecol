/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when assigning a teacher.
 */
public class AssignTeacherMessage extends DOMMessage {

    public static final String TAG = "assignTeacher";
    private static final String STUDENT_TAG = "student";
    private static final String TEACHER_TAG = "teacher";

    /** The identifier of the student. */
    private final String studentId;

    /** The identifier of the teacher. */
    private final String teacherId;


    /**
     * Create a new {@code AssignTeacherMessage} with the
     * supplied student and teacher.
     *
     * @param student The student {@code Unit}.
     * @param teacher The teacher {@code Unit}.
     */
    public AssignTeacherMessage(Unit student, Unit teacher) {
        super(TAG);

        this.studentId = student.getId();
        this.teacherId = teacher.getId();
    }

    /**
     * Create a new {@code AssignTeacherMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AssignTeacherMessage(Game game, Element element) {
        super(TAG);

        this.studentId = getStringAttribute(element, STUDENT_TAG);
        this.teacherId = getStringAttribute(element, TEACHER_TAG);
    }


    /**
     * Handle a "assignTeacher"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message was received on.
     * @return An update containing the student-teacher assignment or
     *     an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit student;
        try {
            student = player.getOurFreeColGameObject(this.studentId, Unit.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Unit teacher;
        try {
            teacher = player.getOurFreeColGameObject(this.teacherId, Unit.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        if (student.getColony() == null) {
            return serverPlayer.clientError("Student not in colony: "
                + this.studentId)
                .build(serverPlayer);
        } else if (!student.isInColony()) {
            return serverPlayer.clientError("Student not working colony: "
                + this.studentId)
                .build(serverPlayer);
        } else if (teacher.getColony() == null) {
            return serverPlayer.clientError("Teacher not in colony: "
                + this.teacherId)
                .build(serverPlayer);
        } else if (!teacher.getColony().canTrain(teacher)) {
            return serverPlayer.clientError("Teacher can not teach: "
                + this.teacherId)
                .build(serverPlayer);
        } else if (student.getColony() != teacher.getColony()) {
            return serverPlayer.clientError("Student and teacher not in same colony: "
                + this.studentId)
                .build(serverPlayer);
        } else if (!student.canBeStudent(teacher)) {
            return serverPlayer.clientError("Student can not be taught by teacher: "
                + this.studentId)
                .build(serverPlayer);
        }

        // Proceed to assign.
        return InGameController
            .assignTeacher(serverPlayer, student, teacher)
            .build(serverPlayer);
    }

    /**
     * Convert this AssignTeacherMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            STUDENT_TAG, this.studentId,
            TEACHER_TAG, this.teacherId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "assignTeacher".
     */
    public static String getTagName() {
        return TAG;
    }
}
