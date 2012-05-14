package org.redpill.alfresco.module.metadatawriter.factories.impl;

import java.io.IOException;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.springframework.util.StringUtils;

public class MetadataContentFactoryImpl implements MetadataContentFactory {

  private final ContentService _contentService;
  private final Set<MetadataContentInstantiator> _instantiators;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public MetadataContentFactoryImpl(final ContentService contentService, final Set<MetadataContentInstantiator> metadataContentInstantiators) {
    assert null != contentService;
    _contentService = contentService;
    _instantiators = metadataContentInstantiators;
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------
  @Override
  public ContentFacade createContent(final NodeRef contentNode) throws UnsupportedMimetypeException, IOException {
    assert null != contentNode;

    final String mimetype = findMimetype(getContentReader(contentNode));

    if (!StringUtils.hasText(mimetype)) {
      throw new ContentIOException("Mimetype not specified for node " + contentNode);
    }

    final MetadataContentInstantiator instantiator = getMetadataContentInstantiator(mimetype);

    if (instantiator == null) {
      throw new UnsupportedMimetypeException("This MetadataContentFactory does not support mimetype " + mimetype);
    }

    return instantiator.create(getContentReader(contentNode), getContentWriter(contentNode));
  }

  @Override
  public boolean supportsMetadataWrite(final NodeRef contentNode) {
    assert null != contentNode;

    final String mimetype = findMimetype(getContentReader(contentNode));

    if (!StringUtils.hasText(mimetype)) {
      return false;
    }

    return getMetadataContentInstantiator(mimetype) != null;
  }

  private MetadataContentInstantiator getMetadataContentInstantiator(final String mimetype) {
    for (final MetadataContentInstantiator instantiator : _instantiators) {
      if (instantiator.supports(mimetype)) {
        return instantiator;
      }
    }

    return null;
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private static String findMimetype(final ContentReader contentReader) {
    if (null == contentReader) {
      throw new ContentIOException("ContentReader must be supplied!");
    }

    return contentReader.getMimetype();
  }

  private ContentReader getContentReader(final NodeRef contentNode) {
    final ContentReader reader = _contentService.getReader(contentNode, ContentModel.PROP_CONTENT);

    // The reader may be null, e.g. for folders and the like
    if (reader == null || !reader.exists()) {
      throw new ContentIOException("Could not get ContentReader from node " + contentNode);
    }

    return reader;
  }

  private ContentWriter getContentWriter(final NodeRef contentNode) {
    final ContentWriter writer = _contentService.getWriter(contentNode, ContentModel.PROP_CONTENT, true);

    if (writer == null) {
      throw new ContentIOException("Could not get ContentWriter from node " + contentNode);
    }

    return writer;
  }

}
