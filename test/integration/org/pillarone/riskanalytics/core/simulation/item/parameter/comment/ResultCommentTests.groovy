package org.pillarone.riskanalytics.core.simulation.item.parameter.comment

import org.pillarone.riskanalytics.core.parameter.comment.CommentTag
import org.pillarone.riskanalytics.core.parameter.comment.ResultCommentDAO
import org.pillarone.riskanalytics.core.parameter.comment.Tag

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class ResultCommentTests extends GroovyTestCase {

    void testConstructor() {
        ResultCommentDAO dao = new ResultCommentDAO(path: "path", periodIndex: 0, comment: "text")

        Comment comment = new Comment(dao)

        assertFalse comment.added
        assertFalse comment.updated
        assertFalse comment.deleted

        assertEquals dao.path, comment.path
        assertEquals dao.periodIndex, comment.period
        assertEquals dao.comment, comment.text

        dao.addToTags(new CommentTag(tag: new Tag(name: "tag")))

        comment = new Comment(dao)
        assertEquals 1, comment.tags.size()

        comment = new Comment("path", 0)
        assertTrue comment.added
        assertEquals "path", comment.path
        assertEquals 0, comment.period
    }

    void testUpdateComment() {
        Comment comment = new Comment("path", 0)
        assertFalse comment.updated

        comment.text = "text"
        assertTrue comment.updated
        assertEquals "text", comment.text
    }

    void testAddRemoveTag() {
        Comment comment = new Comment("path", 0)

        comment.addTag(new Tag(name: "hi"))
        assertEquals 1, comment.tags.size()

        comment.addTag(new Tag(name: "hi"))
        assertEquals 1, comment.tags.size()

        comment.addTag(new Tag(name: "hello"))
        assertEquals 2, comment.tags.size()

        comment.removeTag(new Tag(name: "hello"))
        assertEquals 1, comment.tags.size()

        comment.removeTag(new Tag(name: "hello"))
        assertEquals 1, comment.tags.size()
    }

    void testApplyToDomainObject() {
        Comment comment = new Comment("path", 0)
        comment.text = "text"
        comment.addTag(new Tag(name: "x"))

        ResultCommentDAO dao = new ResultCommentDAO()
        comment.applyToDomainObject(dao)

        assertEquals "path", dao.path
        assertEquals 0, dao.periodIndex
        assertEquals "text", dao.comment
        assertNotNull dao.timeStamp

        assertEquals 1, dao.tags.size()

        comment.addTag(new Tag(name: "y"))
        comment.applyToDomainObject(dao)

        assertEquals 2, dao.tags.size()

        comment.removeTag(new Tag(name: "x"))
        comment.removeTag(new Tag(name: "y"))

        dao = new ResultCommentDAO()
        dao.addToTags(new CommentTag(tag: new Tag(name: "z")))

        comment.applyToDomainObject(dao)
        assertEquals 0, dao.tags.size()

    }
}
