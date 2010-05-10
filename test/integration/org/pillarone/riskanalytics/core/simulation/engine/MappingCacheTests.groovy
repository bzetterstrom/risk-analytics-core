package org.pillarone.riskanalytics.core.simulation.engine

import org.pillarone.riskanalytics.core.example.model.EmptyModel
import org.pillarone.riskanalytics.core.output.CollectorMapping
import org.pillarone.riskanalytics.core.output.FieldMapping
import org.pillarone.riskanalytics.core.output.PathMapping

class MappingCacheTests extends GroovyTestCase {

    void setUp() {
        assertNotNull new PathMapping(pathName: "Empty:path1").save()
        assertNotNull new PathMapping(pathName: "Empty:path2").save()
        assertNotNull new PathMapping(pathName: "Empty:path3").save()
        assertNotNull new PathMapping(pathName: "AnotherModel:path3").save()

        assertNotNull new CollectorMapping(collectorName: "coll").save()
        assertNotNull new FieldMapping(fieldName: "field").save()
    }

    void testInit() {

        MappingCache cache = new MappingCache(new EmptyModel())

        assertEquals 3, cache.paths.size()
        assertEquals 1, cache.fields.size()
        assertEquals 1, cache.collectors.size()
    }

    void testLookup() {
        MappingCache cache = new MappingCache(new EmptyModel())

        assertEquals PathMapping.findByPathName("Empty:path1").id, cache.lookupPath("Empty:path1")
        assertEquals 3, cache.paths.size()

        Long id = cache.lookupPath("Empty:newPath")
        assertEquals "Empty:newPath", PathMapping.get(id).pathName
        assertEquals 4, cache.paths.size()

    }
}
