package com.foerstertechnologies.slickmysql

import slick.ast.{CompiledStatement, Node, ResultSetMapping}
import slick.dbio.{Effect, NoStream}
import slick.jdbc._
import slick.lifted.Query
import slick.relational.{CompiledMapping, ProductResultConverter, ResultConverter, TypeMappingResultConverter}
import slick.util.{ProductWrapper, SQLBuilder}

trait PatchActionExtensionMethodsSupport { profile: JdbcProfile =>

  trait PatchActionImplicits {
    implicit def queryPatchActionExtensionMethods[U <: Product, C[_]](
                                                                       q: Query[_, U, C]
                                                                     ): PatchActionExtensionMethodsImpl[U] =
      createPatchActionExtensionMethods(updateCompiler.run(q.toNode).tree, ())
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////// Patch Actions
  ///////////////////////////////////////////////////////////////////////////////////////////////

  type PatchActionExtensionMethods[T <: Product] = PatchActionExtensionMethodsImpl[T]

  def createPatchActionExtensionMethods[T <: Product](tree: Node, param: Any): PatchActionExtensionMethods[T] =
    new PatchActionExtensionMethodsImpl[T](tree, param)

  class PatchActionExtensionMethodsImpl[T <: Product](tree: Node, param: Any) {
    protected[this] val ResultSetMapping(_, CompiledStatement(_, sres: SQLBuilder.Result, _),
    CompiledMapping(_converter, _)) = tree
    protected[this] val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, Product]]
    protected[this] val TypeMappingResultConverter(childConverter, toBase, toMapped) = converter
    protected[this] val ProductResultConverter(elementConverters @ _ *) =
      childConverter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, Product]]
    private[this] val updateQuerySplitRegExp = """(.*)(?<=set )((?:(?= where)|.)+)(.*)?""".r
    private[this] val updateQuerySetterRegExp = """[^\s]+\s*=\s*\?""".r

    /** An Action that updates the data selected by this query. */
    def patch(value: T): ProfileAction[Int, NoStream, Effect.Write] = {
      val (seq, converters) = value.productIterator.zipWithIndex.toIndexedSeq
        .zip(elementConverters)
        .filter {
          case ((Some(_), _), _) => true
          case ((None, _), _) => false
          case ((null, _), _) => false
          case ((_, _), _) => true
        }
        .unzip

      val (products, indexes) = seq.unzip

      val newConverters = converters.zipWithIndex
        .map(c => (c._1, c._2 + 1))
        .map {
          case (c: BaseResultConverter[_], idx) => new BaseResultConverter(c.ti, c.name, idx)
          case (c: OptionResultConverter[_], idx) => new OptionResultConverter(c.ti, idx)
          case (c: DefaultingResultConverter[_], idx) => new DefaultingResultConverter(c.ti, c.default, idx)
          case (c: IsDefinedResultConverter[_], idx) => new IsDefinedResultConverter(c.ti, idx)
        }

      val productResultConverter =
        ProductResultConverter(newConverters: _*).asInstanceOf[ResultConverter[JdbcResultConverterDomain, Any]]
      val newConverter = TypeMappingResultConverter(productResultConverter, (p: Product) => p, (a: Any) => toMapped(a))

      val newValue: Product = new ProductWrapper(products)
      val newSql = sres.sql match {
        case updateQuerySplitRegExp(prefix, setter, suffix) =>
          val buffer = StringBuilder.newBuilder
          buffer.append(prefix)
          buffer.append(
            updateQuerySetterRegExp
              .findAllIn(setter)
              .zipWithIndex
              .filter(s => indexes.contains(s._2))
              .map(_._1)
              .mkString(", ")
          )
          buffer.append(suffix)
          buffer.toString()
      }

      new SimpleJdbcProfileAction[Int]("patch", Vector(newSql)) {
        def run(ctx: Backend#Context, sql: Vector[String]): Int =
          ctx.session.withPreparedStatement(sql.head) { st =>
            st.clearParameters()
            newConverter.set(newValue, st)
            sres.setter(st, newConverter.width + 1, param)
            st.executeUpdate
          }
      }
    }
  }
}
