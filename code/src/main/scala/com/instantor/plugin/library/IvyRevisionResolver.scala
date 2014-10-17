package com.instantor.plugin
package library

import org.apache.ivy.core.module.descriptor.{ AbstractArtifact, DefaultDependencyDescriptor, DefaultModuleDescriptor, ModuleDescriptor }
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.Ivy
import org.apache.ivy.plugins.resolver.{ DependencyResolver, IBiblioResolver }

object IvyRevisionResolver {
  def resolveLatestVersion(repoName: String, repoRoot: String, scalaVersion: String, sbtVersion: String, groupId: String, artifactId: String): String = {
    val pattern = s"[organisation]/[module]_${ scalaVersion }_${ sbtVersion }/[revision]/[artifact]-[revision](-[classifier]).[ext]"
    val version = "latest.release"

    val resolver = getResolver(repoName, repoRoot, pattern)
    val ivy = getIvy(resolver)
    val resolveOptions = getResolveOptions
    val moduleDescriptor = getModuleDescriptor(groupId, artifactId, version)

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)
    if (resolveReport.hasError) {
      throw new RuntimeException(resolveReport.getAllProblemMessages.toString)
    }

    resolveReport
      .getArtifacts().get(0).asInstanceOf[AbstractArtifact]
      .getId
      .getModuleRevisionId
      .getRevision
  }

  private def getResolver(name: String, root: String, pattern: String): DependencyResolver = {
    val resolver = new IBiblioResolver
    resolver.setM2compatible(true)
    resolver.setUsepoms(true)
    resolver.setName(name)
    resolver.setRoot(root)
    resolver.setPattern(pattern)
    resolver
  }

  private def getIvy(resolver: DependencyResolver): Ivy = {
    val ivySettings = new IvySettings
    ivySettings.addResolver(resolver)
    ivySettings.setDefaultResolver(resolver.getName)
    Ivy.newInstance(ivySettings)
  }

  private def getResolveOptions(): ResolveOptions = {
    val resolveOptions = new ResolveOptions
    resolveOptions.setTransitive(true)
    resolveOptions.setDownload(false)
    resolveOptions.setLog("quiet")
    resolveOptions
  }

  private def getModuleDescriptor(groupId: String, artifactId: String, version: String): ModuleDescriptor = {
    val envelopeRevisionId = ModuleRevisionId.newInstance(groupId, artifactId + "-envelope", version)
    val revisionId         = ModuleRevisionId.newInstance(groupId, artifactId, version)

    val moduleDescriptor     = DefaultModuleDescriptor.newDefaultInstance(envelopeRevisionId)
    val dependencydescriptor = new DefaultDependencyDescriptor(moduleDescriptor, revisionId, false, false, false)

    dependencydescriptor.addDependencyConfiguration("default", "master")
    moduleDescriptor.addDependency(dependencydescriptor)
    moduleDescriptor
  }
}
