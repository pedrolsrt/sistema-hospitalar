package br.pucminas.hospital.exception;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte exceções em respostas ProblemDetail com mensagens em português.
 *
 * <ul>
 * <li>400: dados da requisição inválidos, com a lista de campos em {@code campos};</li>
 * <li>404: recurso não encontrado;</li>
 * <li>409: dado único já cadastrado, transição de situação inválida, violação de integridade e conflito
 * de versão;</li>
 * <li>422: demais regras de negócio;</li>
 * <li>500: erro inesperado, sem detalhes internos.</li>
 * </ul>
 */
// Estende ResponseEntityExceptionHandler para que os erros do próprio Spring MVC (método não suportado,
// tipo de conteúdo etc.) continuem com o status correto em vez de cair no tratamento de erro inesperado.
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
	}

	@ExceptionHandler(DadoJaCadastradoException.class)
	public ProblemDetail dadoJaCadastrado(DadoJaCadastradoException ex) {
		return problema(HttpStatus.CONFLICT, "Dado já cadastrado", ex.getMessage());
	}

	@ExceptionHandler(TransicaoSituacaoInvalidaException.class)
	public ProblemDetail transicaoInvalida(TransicaoSituacaoInvalidaException ex) {
		return problema(HttpStatus.CONFLICT, "Transição de situação inválida", ex.getMessage());
	}

	@ExceptionHandler(RegraNegocioException.class)
	public ProblemDetail regraNegocio(RegraNegocioException ex) {
		return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", ex.getMessage());
	}

	// Normalmente os services verificam antes de gravar; isto cobre o que escapa dessas verificações,
	// como dois cadastros simultâneos com o mesmo CPF. A mensagem do banco não é exposta.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail integridade(DataIntegrityViolationException ex) {
		log.warn("Violação de integridade dos dados", ex);
		return problema(HttpStatus.CONFLICT, "Conflito de dados",
				"A operação não pôde ser concluída porque entra em conflito com dados já cadastrados.");
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail concorrencia(OptimisticLockingFailureException ex) {
		log.info("Conflito de versão: {}", ex.getMessage());
		return problema(HttpStatus.CONFLICT, "Alteração simultânea",
				"Os dados foram alterados por outra operação ao mesmo tempo. Consulte novamente e repita a operação.");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail erroInesperado(Exception ex) {
		log.error("Erro inesperado", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado",
				"Ocorreu um erro inesperado. Tente novamente mais tarde.");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<CampoInvalido> campos = new ArrayList<>();
		for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
			campos.add(new CampoInvalido(erro.getField(), erro.getDefaultMessage()));
		}
		ex.getBindingResult().getGlobalErrors()
				.forEach(erro -> campos.add(new CampoInvalido(erro.getObjectName(), erro.getDefaultMessage())));
		return dadosInvalidos(campos);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<CampoInvalido> campos = new ArrayList<>();
		ex.getParameterValidationResults().forEach(resultado -> {
			String parametro = resultado.getMethodParameter().getParameterName();
			for (MessageSourceResolvable erro : resultado.getResolvableErrors()) {
				campos.add(new CampoInvalido(parametro, erro.getDefaultMessage()));
			}
		});
		return dadosInvalidos(campos);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return resposta(problema(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"O corpo da requisição está mal formatado ou contém um valor não aceito."));
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return resposta(problema(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"O valor '" + ex.getValue() + "' não é aceito para o parâmetro " + ex.getPropertyName() + "."));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return resposta(problema(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"O parâmetro " + ex.getParameterName() + " é obrigatório."));
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return resposta(problema(HttpStatus.NOT_FOUND, "Recurso não encontrado",
				"O endereço " + ex.getResourcePath() + " não existe."));
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers)
				.body(problema(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
						"O método " + ex.getMethod() + " não é aceito neste endereço."));
	}

	private ResponseEntity<Object> dadosInvalidos(List<CampoInvalido> campos) {
		campos.sort(Comparator.comparing(CampoInvalido::campo).thenComparing(CampoInvalido::mensagem));
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Um ou mais campos da requisição estão inválidos.");
		problema.setProperty("campos", campos);
		return resposta(problema);
	}

	private static ResponseEntity<Object> resposta(ProblemDetail problema) {
		return ResponseEntity.status(problema.getStatus()).body(problema);
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

}
