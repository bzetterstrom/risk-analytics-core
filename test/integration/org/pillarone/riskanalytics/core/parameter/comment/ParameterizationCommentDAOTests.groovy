package org.pillarone.riskanalytics.core.parameter.comment

import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.example.model.EmptyModel
import org.pillarone.riskanalytics.core.workflow.Status

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class ParameterizationCommentDAOTests extends GroovyTestCase {

    ParameterizationDAO parameterization


    @Override
    protected void setUp() {
        parameterization = new ParameterizationDAO()
        parameterization.name = "test"
        parameterization.modelClassName = EmptyModel.name
        parameterization.itemVersion = "1"
        parameterization.periodCount = 1
        parameterization.status = Status.NONE
        assertNotNull parameterization.save()
    }



    void testSaveDeleteP14NCommentDAO() {
        int tagCount = Tag.count()
        int commentCount = CommentDAO.count()
        int commentTagCount = CommentTag.count()

        Tag tag = new Tag(name: 'tag').save()
        assertNotNull tag

        ParameterizationCommentDAO comment = new ParameterizationCommentDAO()
        comment.parameterization = parameterization
        comment.path = "path"
        comment.periodIndex = 0
        comment.timeStamp = new DateTime()
        comment.comment = "text"

        comment.addToTags(new CommentTag(tag: tag))
        comment.files = ["file1", "file2"] as Set

        assertNotNull comment.save()

        assertEquals tagCount + 1, Tag.count()
        assertEquals commentCount + 1, ParameterizationCommentDAO.count()
        assertEquals commentTagCount + 1, CommentTag.count()

        comment.delete()

        assertEquals tagCount + 1, Tag.count()
        assertEquals commentCount, ParameterizationCommentDAO.count()
        assertEquals commentTagCount, CommentTag.count()

    }
}
