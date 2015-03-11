package org.aksw.gerbil.utils.bat;

import it.acubelab.batframework.data.Mention;
import it.acubelab.batframework.data.ScoredTag;
import it.acubelab.batframework.data.Tag;
import it.acubelab.batframework.utils.WikipediaApiInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.Annotation;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.gerbil.transfer.nif.data.ScoredAnnotation;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;
import org.aksw.gerbil.transfer.nif.data.SpanImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BAT2NIF_TranslationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BAT2NIF_TranslationHelper.class);

    private static final String DBPEDIA_URI = "http://dbpedia.org/resource/";

    private WikipediaApiInterface wikiApi;

    public BAT2NIF_TranslationHelper(WikipediaApiInterface wikiApi) {
        super();
        this.wikiApi = wikiApi;
    }

    public Document createDocument(String text) {
        return new DocumentImpl(text);
    }

    public Document createDocumentWithMentions(String text, Set<Mention> mentions) {
        return new DocumentImpl(text, new ArrayList<Marking>(translateMentions(mentions)));
    }

    public Document createDocumentWithTags(String text, Set<Tag> mentions) {
        return new DocumentImpl(text, new ArrayList<Marking>(translateTags(mentions)));
    }

    public Document createDocumentWithAnnotations(String text, Set<it.acubelab.batframework.data.Annotation> annotations) {
        return new DocumentImpl(text, new ArrayList<Marking>(translateAnnotations(annotations)));
    }

    public List<Span> translateMentions(Set<Mention> mentions) {
        List<Span> markings = new ArrayList<Span>();
        if (mentions != null) {
            for (Mention mention : mentions) {
                markings.add(translate(mention));
            }
        }
        return markings;
    }

    public List<Meaning> translateTags(Set<Tag> tags) {
        List<Meaning> markings = new ArrayList<Meaning>();
        if (tags != null) {
            for (Tag tag : tags) {
                if (tag instanceof ScoredTag) {
                    markings.add(translate((ScoredTag) tag));
                } else {
                    markings.add(translate(tag));
                }
            }
        }
        return markings;
    }

    public List<NamedEntity> translateAnnotations(Set<it.acubelab.batframework.data.Annotation> annotations) {
        List<NamedEntity> markings = new ArrayList<NamedEntity>();
        if (annotations != null) {
            for (it.acubelab.batframework.data.Annotation a : annotations) {
                if (a instanceof it.acubelab.batframework.data.ScoredAnnotation) {
                    markings.add(translate((it.acubelab.batframework.data.ScoredAnnotation) a));
                } else {
                    markings.add(translate(a));
                }
            }
        }
        return markings;
    }

    public Span translate(Mention mention) {
        return new SpanImpl(mention.getPosition(), mention.getLength());
    }

    public Meaning translate(Tag tag) {
        return new Annotation(translateWId(tag.getConcept()));
    }

    public Meaning translate(ScoredTag tag) {
        return new ScoredAnnotation(translateWId(tag.getConcept()), tag.getScore());
    }

    public NamedEntity translate(it.acubelab.batframework.data.Annotation annotation) {
        return new NamedEntity(annotation.getPosition(), annotation.getLength(), translateWId(annotation.getConcept()));
    }

    public NamedEntity translate(it.acubelab.batframework.data.ScoredAnnotation annotation) {
        return new ScoredNamedEntity(annotation.getPosition(), annotation.getLength(),
                translateWId(annotation.getConcept()), annotation.getScore());
    }

    public String translateWId(int id) {
        if (id < 0) {
            return null;
        }
        String title;
        try {
            title = wikiApi.getTitlebyId(id);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving title for given Wikipedia Id. Returning null.", e);
            return null;
        }
        if (title == null) {
            return null;
        } else {
            return DBPEDIA_URI + title;
        }
    }
}