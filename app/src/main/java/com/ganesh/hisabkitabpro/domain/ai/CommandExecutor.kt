package com.ganesh.hisabkitabpro.domain.ai

data class ExecutionResult(
    val message: String,
    val action: String? = null
)

object CommandExecutor {

    fun execute(
        parsed: ParsedCommand
    ): ExecutionResult {

        return when (parsed.intent) {

            CommandIntent.SEND_LEDGER -> {

                ExecutionResult(
                    message = "Preparing customer ledger...",
                    action = "OPEN_LEDGER"
                )
            }

            CommandIntent.SEND_INVOICE -> {

                ExecutionResult(
                    message = "Preparing invoice...",
                    action = "OPEN_INVOICE"
                )
            }

            CommandIntent.ADD_TRANSACTION -> {

                if (parsed.customerName == null || parsed.amount == null) {

                    ExecutionResult(
                        message = "Please specify customer and amount"
                    )

                } else {

                    ExecutionResult(
                        message = "Adding ₹${parsed.amount} for ${parsed.customerName}",
                        action = "CREATE_TRANSACTION"
                    )
                }
            }

            CommandIntent.CHANGE_LANGUAGE -> {

                ExecutionResult(
                    message = "Opening language settings...",
                    action = "OPEN_LANGUAGE"
                )
            }

            CommandIntent.OPEN_CUSTOMER -> {

                ExecutionResult(
                    message = "Opening customer...",
                    action = "OPEN_CUSTOMER"
                )
            }

            CommandIntent.SHOW_ANALYTICS -> {

                ExecutionResult(
                    message = "Opening analytics...",
                    action = "OPEN_ANALYTICS"
                )
            }

            CommandIntent.UNKNOWN -> {

                ExecutionResult(
                    message = "Sorry, I didn't understand."
                )
            }
        }
    }
}