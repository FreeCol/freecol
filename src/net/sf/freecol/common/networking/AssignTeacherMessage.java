/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when assigning a teacher.
 */
public class AssignTeacherMessage extends DOMMessage {

    /** The identifier of the student. */
    private final String studentId;

    /** The identifier of the teacher. */
    private final String teacherId;


    /**
     * Create a new <code>AssignTeacherMessage</code> with the
     * supplied student and teacher.
     *
     * @param student The student <code>Unit</code>.
     * @param teacher The teacher <code>Unit</code>.
     */
    public AssignTeacherMessage(Unit student, Unit teacher) {
        super(getXMLElementTagName());

        this.studentId = student.getId();
        this.teacherId = teacher.getId();
    }

    /**
     * Create a new <code>AssignTeacherMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public AssignTeacherMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.studentId = element.getAttribute("student");
        this.teacherId = element.getAttribute("teacher");
    }


    /**
     * Handle a "assignTeacher"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the student-teacher assignment or
     *     an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit student;
        try {
            student = player.getOurFreeColGameObject(studentId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Unit teacher;
        try {
            teacher = player.getOurFreeColGameObject(teacherId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        if (student.getColony() == null) {
            return DOMMessage.clientError("Student not in colony: "
                + studentId);
        } else if (!student.isInColony()) {
            return DOMMessage.clientError("Student not working colony: "
                + studentId);
        } else if (teacher.getColony() == null) {
            return DOMMessage.clientError("Teacher not in colony: "
                + teacherId);
        } else if (!teacher.getColony().canTrain(teacher)) {
            return DOMMessage.clientError("Teacher can not teach: "
                + teacherId);
        } else if (student.getColony() != teacher.getColony()) {
            return DOMMessage.clientError("Student and teacher not in same colony: "
                + studentId);
        } else if (!student.canBeStudent(teacher)) {
            return DOMMessage.clientError("Student can not be taught by teacher: "
                + studentId);
        }

        // Proceed to assign.
        return server.getInGameController()
            .assignTeacher(serverPlayer, student, teacher);
    }

    /**
     * Convert this AssignTeacherMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "student", studentId,
            "teacher", teacherId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "assignTeacher".
     */
    public static String getXMLElementTagName() {
        return "assignTeacher";
    }
}
