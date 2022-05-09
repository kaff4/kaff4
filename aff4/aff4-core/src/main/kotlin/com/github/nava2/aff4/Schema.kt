package com.github.nava2.aff4

import java.util.UUID

object AFF4 {
  /**
   * The base URI for AFF4 Standard v1.0.
   */
  const val AFF4_BASE_URI = "http://aff4.org/Schema#"

  /**
   * Typical Prefix for aff4 URN. (eg "aff4://").
   */
  const val AFF4_URN_PREFIX = "aff4://"

  /**
   * RDF Prefix type for JENA.
   */
  const val AFF4_RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  /**
   * Black Bag Technologies base URI for custom properties
   */
  const val BBT_BASE_URI = "https://blackbagtech.com/aff4/Schema#"

  /**
   * The default chunk size
   */
  const val DEFAULT_CHUNK_SIZE = 32 * 1024

  /**
   * The default number of chunks for each segment/bevvy instance.
   */
  const val DEFAULT_CHUNKS_PER_SEGMENT = 2 * 1024

  /**
   * The default number of bytes per sector.
   */
  const val BYTES_PER_SECTOR_DEFAULT = 512

  /**
   * The default filename extension for AFF4 files.
   */
  const val DEFAULT_AFF4_EXTENSION = ".aff4"

  /**
   * Filename of the TURTLE file being stored.
   */
  const val INFORMATIONTURTLE = "information.turtle"

  /**
   * Filename of the container description file used to hold the Volume AFF4 resource ID.
   */
  const val FILEDESCRIPTOR = "container.description"

  /**
   * Version file.
   */
  const val VERSIONDESCRIPTIONFILE = "version.txt"

  /**
   * Generate a new random AFF4 UUID resource ID as a string
   *
   * @return A new aff4 resource ID.
   */
  fun generateID(): String {
    return AFF4_URN_PREFIX + UUID.randomUUID().toString()
  }
}