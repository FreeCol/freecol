/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
public class AssignTeacherMessage extends AttributeMessage {

    public static final String TAG = "assignTeacher";
    private static final String STUDENT_TAG = "student";
    private static final String TEACHER_TAG = "teacher";


    /**
     * Create a new {@code AssignTeacherMessage} with the
     * supplied student and teacher.
     *
     * @param student The student {@code Unit}.
     * @param teacher The teacher {@code Unit}.
     */
    public AssignTeacherMessage(Unit student, Unit teacher) {
        super(TAG, STUDENT_TAG, student.getId(), TEACHER_TAG, teacher.getId());
    }

    /**
     * Create a new {@code AssignTeacherMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AssignTeacherMessage(Game game, Element element) {
        super(TAG, STUDENT_TAG, getStringAttribute(element, STUDENT_TAG),
              TEACHER_TAG, getStringAttribute(element, TEACHER_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String studentId = getStringAttribute(STUDENT_TAG);
        final String teacherId = getStringAttribute(TEACHER_TAG);

        Unit student;
        try {
            student = serverPlayer.getOurFreeColGameObject(studentId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Unit teacher;
        try {
            teacher = serverPlayer.getOurFreeColGameObject(teacherId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        if (student.getColony() == null) {
            return serverPlayer.clientError("Student not in colony: "
                + studentId);
        } else if (!student.isInColony()) {
            return serverPlayer.clientError("Student not working colony: "
                + studentId);
        } else if (teacher.getColony() == null) {
            return serverPlayer.clientError("Teacher not in colony: "
                + teacherId);
        } else if (!teacher.getColony().canTrain(teacher)) {
            return serverPlayer.clientError("Teacher can not teach: "
                + teacherId);
        } else if (student.getColony() != teacher.getColony()) {
            return serverPlayer.clientError("Student and teacher not in same colony: "
                + studentId);
        } else if (!student.canBeStudent(teacher)) {
            return serverPlayer.clientError("Student can not be taught by teacher: "
                + studentId);
        }

        // Proceed to assign.
        return freeColServer.getInGameController()
            .assignTeacher(serverPlayer, student, teacher);
    }
}
