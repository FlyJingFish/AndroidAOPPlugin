package io.github.FlyJingFish.AndroidAopPlugin.openclassfiles._internal

import io.github.FlyJingFish.AndroidAopPlugin.common.SourceFile.CompilableSourceFile
import io.github.FlyJingFish.AndroidAopPlugin.openclassfiles._internal.ClassFileCandidates.AbsoluteClassFileCandidates

data class ClassFilePreparationTask(
    val compilerOutputClassFileCandidates: AbsoluteClassFileCandidates,
    val sourceFile: CompilableSourceFile
  )