package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired



/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,

) : CreateShortUrlUseCase {
    @Autowired
    lateinit var validateURIUseCase: ValidateURIUseCase

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (data.leftUses != null && data.leftUses <= 0) throw InvalidLeftUses(data.leftUses.toString())
        else if(validatorService.isValid(url)) {
            runBlocking {
                val validationResponse = async { validateURIUseCase.ValidateURI(url) }

                var id: String = hashService.hasUrl(url)
                if (data.leftUses != null || data.expiration != null) {
                    id += hashService.hasUrl((System.currentTimeMillis()).toString())
                }
                var su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    validation = ValidateURISTATUS.VALIDATION_IN_PROGRESS,
                    properties = ShortUrlProperties(
                        safe = data.safe,
                        ip = data.ip,
                        sponsor = data.sponsor,
                        leftUses = data.leftUses,
                        expiration = data.expiration
                    )
                )
                shortUrlRepository.save(su)
                var job = GlobalScope.launch {
                    runBlocking {
                        print("INBLOCK")
                        if (validationResponse.await() == ValidateURIUseCaseResponse.OK) {
                            var validationResult = ValidateURISTATUS.VALIDATION_PASS
                            delay(3000)
                            //Delay para darme tiempo a darle a una uri aún no validada
                            //También para que tarde mas que el delay de abajo
                            print("PASSED")
                            val res = shortUrlRepository.updateValidation(su.hash,validationResult)
                            print(res)
                        }
                        if (validationResponse.await() == ValidateURIUseCaseResponse.UNSAFE) {
                            print("UNSAFE")
                            shortUrlRepository.deleteByKey(su.hash)
                        }
                        if (validationResponse.await() == ValidateURIUseCaseResponse.NOT_REACHABLE) {
                            print("UNREACHEABLE")
                            shortUrlRepository.deleteByKey(su.hash)
                        }
                        print("OUTBLOCK")
                    }
                }
                delay(1000)
                //Este delay no me gusta nada, pero a partir de la 2ª ejecución, cancel va antes del launch y falla
                //espero 1s para que el launch acabe
                job.cancel()
                print("CANCEL")
                su
            }
        } else throw InvalidUrlException(url)
}

