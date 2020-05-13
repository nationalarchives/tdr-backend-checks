package uk.gov.nationalarchives.api.update.common.exceptions

import uk.gov.nationalarchives.tdr.error.GraphQlError

class GraphQlException(errors: List[GraphQlError])
  extends RuntimeException(s"GraphQL response contained errors: ${errors.map(e => e.message).mkString}")
