package com.jtransc

import com.jtransc.ds.diff
import com.jtransc.ds.hasAnyFlags
import com.jtransc.ds.hasFlag
import com.jtransc.maven.MavenLocalRepository
import com.jtransc.vfs.GetClassJar
import com.jtransc.vfs.MergedLocalAndJars
import com.jtransc.vfs.SyncVfsFile
import com.jtransc.vfs.ZipVfs
import jtransc.JTranscVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class JTranscRtReport {
	companion object {
		@JvmStatic fun main(args: Array<String>) {
			JTranscRtReport().report()
		}
	}

	val jtranscVersion = JTranscVersion.getVersion()
	val javaRt = ZipVfs(GetClassJar(String::class.java))
	val jtranscRt = MergedLocalAndJars(MavenLocalRepository.locateJars("com.jtransc:jtransc-rt:$jtranscVersion"))

	fun report() {
		reportPackage("java", listOf(
			"java.rmi", "java.sql", "java.beans", "java.awt", "java.applet"
		))
		//reportPackage("java.lang")
		//reportPackage("java.io")
		//reportPackage("java.util")
		/*
		for (e in javaRt.listdirRecursive().filter { it.name.endsWith(".class") }) {
			println(e.path)
		}
		*/
		//for (e in jtranscRt.listdirRecursive().filter { it.name.endsWith(".class") }) {
		//	println(e.path)
		//}
	}

	fun reportPackage(packageName: String, ignoreSet: List<String>) {
		val ignoreSetNormalized = ignoreSet.map { it.replace('.', '/') }
		val packagePath = packageName.replace('.', '/')
		fileList@for (e in javaRt[packagePath].listdirRecursive().filter { it.name.endsWith(".class") }) {
			//for (e.file)
			for (base in ignoreSetNormalized) {
				if (e.file.path.startsWith(base)) {
					continue@fileList
				}
			}
			compareFiles(e.file, jtranscRt[e.path])
		}
	}

	fun compareFiles(f1: SyncVfsFile, f2: SyncVfsFile) {
		//interface MemberRef {}
		data class MethodRef(val name: String, val desc: String)

		fun readClass(data: ByteArray): ClassNode {
			return ClassNode(Opcodes.ASM5).apply {
				ClassReader(data).accept(this, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES)
			}
		}

		fun ClassNode.getPublicOrProtectedMethodDescs() = this.methods
			.filterIsInstance<MethodNode>()
			.filter { it.access hasAnyFlags (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) }
			.map { MethodRef(it.name, it.desc) }

		if (f1.exists) {
			val javaClass = readClass(f1.readBytes())
			if (javaClass.access hasAnyFlags Opcodes.ACC_PUBLIC) {
				if (f2.exists) {
					val jtranscClass = readClass(f2.readBytes())

					val javaMethods = javaClass.getPublicOrProtectedMethodDescs()
					val jtranscMethods = jtranscClass.getPublicOrProtectedMethodDescs()

					val javaFields = javaClass.getPublicOrProtectedMethodDescs()
					val jtranscFields = jtranscClass.getPublicOrProtectedMethodDescs()

					val result = javaMethods.diff(jtranscMethods)

					if (result.justFirst.isNotEmpty()) {
						println("${javaClass.name} (missing methods)")
						for (i in result.justFirst) {
							println(" - ${i.name} : ${i.desc}")
						}
					}
				} else {
					println("${javaClass.name} (missing class)")
				}
			}
			//for (node in origClass.methods.filterIsInstance<MethodNode>()) node.name + node.desc
			//println("" + f1.path + " : " + f2.path)
		}
	}
}