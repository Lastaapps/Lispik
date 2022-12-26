package data

import domain.model.Error

fun Error.getMessage(): String = when (this) {
    is Error.TokenError -> getMessage()
    is Error.ParserError -> getMessage()
    is Error.CompilerError -> getMessage()
    is Error.ExecutionError -> getMessage()
}

fun Error.TokenError.getMessage(): String = when (this) {
    is Error.TokenError.CommentWrongFormat -> "Wrong comment format at $pos"
    is Error.TokenError.EOFReached -> "EOR reached, but not expected at $pos"
    is Error.TokenError.UnclosedComment -> "Unclosed multiline comment"
    is Error.TokenError.UnknownCharacter -> "Unknown character '$char' at $pos"
}

fun Error.ParserError.getMessage(): String = when (this) {
    Error.ParserError.ApplyEmpty -> "Apply args are empty"
    Error.ParserError.ApplyTargetMissingOrInvalid -> "Apply target not found"
    Error.ParserError.DeFunInNonRootScope -> "Function can be defined only in the root scope"
    Error.ParserError.EndReached -> "No more tokens, but some are required"
    is Error.ParserError.FunctionDefinedTwice -> "Function '$name' is already defined"
    is Error.ParserError.InvalidNumberOfArgumentsBuildIn -> "Expected $expected, got $got while handling build-in $token"
    is Error.ParserError.InvalidNumberOfArgumentsOperator -> "Expected $expected, got $got while handling operator $token"
    Error.ParserError.LiteralsOnly -> "Only literals are allowed"
    is Error.ParserError.NameMissing -> "Name is missing for ${token.token} at ${token.position}"
    is Error.ParserError.UnexpectedToken -> "An unexpected token occurred: ${token.token} at ${token.position}"
}

fun Error.CompilerError.getMessage(): String = when (this) {
    Error.CompilerError.ApplyArgsCannotBeEmpty -> "Apply requires at least one argument"
    Error.CompilerError.ApplyOnBuildInsNotSupported -> "Apply called on build-in functions is not supported"
    Error.CompilerError.FunctionsUsedWithoutGlobalEnv -> "To define functions, enable global env"
    is Error.CompilerError.NotFoundByName -> "Parameter $name is not defined"
}

fun Error.ExecutionError.getMessage(): String = when (this) {
    is Error.ExecutionError.CannotRestoreOldContext -> "Cannot restore old context"
    is Error.ExecutionError.CodeNotEmptyOnJoin -> "Nothing to join"
    Error.ExecutionError.DivisionByZero -> "Division by zero"
    Error.ExecutionError.InvalidEnvTargetFormat -> "Invalid environment format"
    Error.ExecutionError.ListWrongFormatOrIndexOfBound -> "Wrong format or index of bound"
    is Error.ExecutionError.NonInstructionOccurred -> "You cannot execute values, only instructions"
    is Error.ExecutionError.NotEnoughOperandsOnStack -> "Not enough operands on stack for $instruction, expected $expected, got $got"
    Error.ExecutionError.NothingToTakeFromDump -> "Nothing to restore from dump"
    is Error.ExecutionError.ReadInvalidNumberOfTokens -> "Read invalid number of tokens"
    Error.ExecutionError.RemovedEnvInsteadOfDummy -> "Tried to remove actual environment instead of dummy"
    is Error.ExecutionError.WrongOperandInByteCode -> "Unexpected operand type for $instruction in code, expected ${expected.simpleName}, got ${got.simpleName}"
    is Error.ExecutionError.WrongOperandOnStack -> "Unexpected operand type for $instruction on stack, expected ${expected.simpleName}, got ${got.simpleName}"
}
