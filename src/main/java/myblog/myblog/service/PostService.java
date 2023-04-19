package myblog.myblog.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import myblog.myblog.domain.Member;
import myblog.myblog.domain.Post;
import myblog.myblog.dto.BasicResponseDTO;
import myblog.myblog.dto.post.PostRequestDTO;
import myblog.myblog.dto.post.PostResponseDTO;
import myblog.myblog.repository.MemberRepository;
import myblog.myblog.repository.PostRepository;
import myblog.myblog.security.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;

    /**
     * 전체 게시글 조회
     */
    public ResponseEntity list() {
        //수정날짜 기준 내림차순
        //Post 객체를 PostResponseDTO 타입으로 변경하여 리스트로 반환
        List<PostResponseDTO> postResponseDTOS = postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PostResponseDTO::new)
                .collect(Collectors.toList());
        BasicResponseDTO basicResponseDTO = BasicResponseDTO.setSuccess("list success", postResponseDTOS);
        return new ResponseEntity(basicResponseDTO, HttpStatus.OK);
    }

    /**
     * 게시글 등록
     */
    @Transactional
    public ResponseEntity savePost(PostRequestDTO postRequestDTO, HttpServletRequest request) {
        String username = getUsernameFromToken(request);
        Post post = new Post(postRequestDTO);

        // 회원 레포지토리에서 회원 가져오기
        Member member = checkMember(username);

        post.setMember(member);
        postRepository.save(post);
        BasicResponseDTO basicResponseDTO = BasicResponseDTO.setSuccess("save success", new PostResponseDTO(post));
        return new ResponseEntity(basicResponseDTO, HttpStatus.OK);
    }

    /**
     * 특정 게시글 조회
     */
    public ResponseEntity findPostById(Long id) {
        //게시글 존재 여부 확인
        Post post = checkPost(id);
        BasicResponseDTO basicResponseDTO = BasicResponseDTO.setSuccess("findOne success", new PostResponseDTO(post));
        return new ResponseEntity(basicResponseDTO, HttpStatus.OK);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public ResponseEntity deletePost(Long id, HttpServletRequest request) {

        String username = getUsernameFromToken(request);

        // 회원 레포지토리에서 회원 가져오기
        Member member = checkMember(username);

        //게시글 존재 여부 확인
        Post post = checkPost(id);

        //작성자의 게시글인지 확인
        isPostAuthor(member, post);

        postRepository.deleteById(id);
        BasicResponseDTO basicResponseDTO = BasicResponseDTO.setSuccess("delete success", null);
        return new ResponseEntity(basicResponseDTO, HttpStatus.OK);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public ResponseEntity updatePost(Long id, PostRequestDTO postRequestDTO, HttpServletRequest request) {
        String username = getUsernameFromToken(request);
        Post post = new Post(postRequestDTO);

        // 회원 레포지토리에서 회원 가져오기
        Member member = checkMember(username);

        // 게시글 존재 여부 확인
        checkPost(id);

        //작성자의 게시글인지 확인
        isPostAuthor(member, post);

        post.update(postRequestDTO);
        BasicResponseDTO basicResponseDTO = BasicResponseDTO.setSuccess("update success", new PostResponseDTO(post));
        return new ResponseEntity(basicResponseDTO, HttpStatus.OK);
    }

    //게시글 존재 여부 확인
    private Post checkPost(Long id) {
        return postRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("게시글이 존재하지 않습니다.")
        );
    }

    //회원 존재 여부 확인
    private Member checkMember(String username) {
        return memberRepository.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("사용자가 존재하지 않습니다.")
        );
    }

    //작성자 일치 여부 판단
    private void isPostAuthor(Member member, Post post) {
        if (post.getMember() != member) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
    }

    //토큰에서 사용자 정보 가져오기
    private String getUsernameFromToken(HttpServletRequest request) {
        String token = tokenProvider.resolveToken(request);
        return tokenProvider.validate(token);
    }
}
