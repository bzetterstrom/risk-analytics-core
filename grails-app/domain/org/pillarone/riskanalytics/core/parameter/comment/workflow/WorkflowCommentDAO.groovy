package org.pillarone.riskanalytics.core.parameter.comment.workflow

import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.user.Person
import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.persistence.DateTimeMillisUserType

class WorkflowCommentDAO {

    ParameterizationDAO parameterization
    String path
    int periodIndex
    DateTime timeStamp
    String comment
    Person user
    IssueStatus status

    static belongsTo = ParameterizationDAO

    static constraints = {
        user(nullable: true)
    }

    String toString() {
        "$path P$periodIndex: $comment"
    }

    static mapping = {
        timeStamp type: DateTimeMillisUserType
    }
}
