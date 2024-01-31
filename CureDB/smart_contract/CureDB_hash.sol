//SPDX-License-Identifier: MIT
pragma solidity ^0.8.16;

contract Sqbf{
    struct Tag {
        uint tokenTag;
        uint[] voTag;
    }
    event LogBool(bool);
    mapping(bytes => bytes) private rootHashes;

    event LogBytes1(bytes1);
    event LogInt(uint);
    event LogString(string);

    function updRootHash(bytes memory tau_w, bytes memory rootHash) public {
        rootHashes[tau_w] = rootHash;
    }


    function verifyRes(bytes[] memory tau_w_list, bytes[] calldata vo, bytes memory token, bytes[] memory res) public {
        Tag memory tag;
        uint n = tau_w_list.length;
        tag.voTag = new uint[](n);
        (bytes[] memory hash, bytes memory res) = verify_for_tree(token, vo, tag);
        bool isPass = true;
        for (uint i = 0; i < n; ++i) {
            bytes memory rootHash = rootHashes[tau_w_list[i]];
            isPass = isPass && isBytesEqual(rootHash, hash[i]);
        }
        emit LogBool(isPass);
    }

    // vo: <,,<3,hash,,hash>,>

    function tree_ver_test(bytes memory token, bytes[] calldata vo) public returns (bytes[] memory hash, bytes memory res) {
        uint n = vo.length;
        Tag memory tag;
        tag.voTag = new uint[](n);
        return verify_for_tree(token, vo, tag);
    }

    function verify_for_tree(bytes memory token, bytes[] calldata vo, Tag memory tag) internal returns (bytes[] memory hash, bytes memory res) {
        if (token.length != 0) {
            tag.tokenTag++;//token:<
            if (token[tag.tokenTag] == '>') {
                tag.tokenTag++;//>
                token = "";
            }
        }  
        uint t;
        uint n = vo.length;
        hash = new bytes[](n);
        // vo[i].length = tag.voTag[i] 表示该子树不需要遍历//vo:<
        bytes memory id = "0";
        uint count = 0;

        uint[] memory tmpTag = new uint[](n); 

        for (uint i = 0; i < n; ++i) {
            if (tag.voTag[i] != vo[i].length) {
                if (vo[i][tag.voTag[i]] == '<') {
                    tag.voTag[i]++;
                    id = "";//标记一下，表示还有子节点需要遍历
                    continue;
                }
                t = tag.voTag[i];
                while (vo[i][t] != ';' && vo[i][t] != '>') t++;
                if (tag.voTag[i] == t) {//没有该关键字
                    hash[i] = abi.encodePacked(keccak256(""));
                    tmpTag[i] = tag.voTag[i] + 1;//记录接下来不需要遍历这个关键字VO
                } else {//叶节点有关键字
                    id = (vo[i][tag.voTag[i] : t]);
                    hash[i] = abi.encodePacked(keccak256(id));
                    tag.voTag[i] = t;
                    count++;
                }
                tag.voTag[i]++;//跳过','或者'>'
            }
        }
        if (count == n) res = abi.encodePacked(res, id, ";");//叶节点是查询结果
        if (id.length != 0) return (hash, res);//没有孩子节点需要遍历

        //对于不存在的关键字，接下来遍历的tag置为vo[i].length
        for (uint i = 0; i < n; ++i) {
            if (tmpTag[i] != 0) tag.voTag[i] = vo[i].length;
        }

        bytes[] memory chdHash = new bytes[](n);
        //遍历孩子节点
        for (uint c = 0; c < 4; ++c) {
            if (token.length == 0 || token[tag.tokenTag] == '<') {//遍历c孩子节点
                (bytes[] memory subHash, bytes memory subRes) = verify_for_tree(token, vo, tag);
                if (token.length != 0) tag.tokenTag++;//;
                res = abi.encodePacked(res, subRes, ";");
                for (uint i = 0; i < n; ++i) {
                    if (tag.voTag[i] != vo[i].length) {
                        chdHash[i] = abi.encodePacked(chdHash[i], subHash[i]);
                        tag.voTag[i]++;//跳过','或者'>'
                    }
                }
            } else {//token[tag.tokenTag] == ';'，表示不需要遍历
                tag.tokenTag++;
                for (uint i = 0; i < n; ++i) {
                    if (tag.voTag[i] != vo[i].length) {
                        chdHash[i] = abi.encodePacked(chdHash[i], vo[i][tag.voTag[i] : tag.voTag[i] + 32]);
                        tag.voTag[i] += 32;
                        tag.voTag[i]++;//跳过';'或者'>'
                    }
                }
            }
        }
        //返回该节点哈希值
        for (uint i = 0; i < n; ++i) {
            if (vo[i].length != 0) {
                hash[i] = abi.encodePacked(keccak256(chdHash[i]));
            }
        }
        //对于更改的voTag[i]，再回溯更改
        for (uint i = 0; i < n; ++i) {
            if (tmpTag[i] != 0) tag.voTag[i] = tmpTag[i];
        }
        return (hash, res);
    }

    function isBytesEqual(bytes memory b1, bytes memory b2) pure internal returns(bool ans) {
        uint n1 = b1.length;
        uint n2 = b2.length;
        if (n1 != n2) return false;
        for (uint i = 0; i < n1; ++i) {
            if (b1[i] != b2[i]) return false;
        }
        return true;
    }

    function computeRootHashAndLoad(bytes memory tau_w, bytes calldata input) public {
        (bytes memory hash, ) = computeRootHash(input, 0);
        updRootHash(tau_w, hash);
    }

    function computeRootHash(bytes calldata input, uint tag) public pure returns(bytes memory res, uint) {
        if (input[tag] == '<') {
            tag++;
        } else {
            uint t = tag;
            while (t < input.length && input[t] != ';' && input[t] != '>') t++;
            if (t == tag) {//','，没有关键字
                res = abi.encodePacked(keccak256(""));
            } else if (t - tag < 32){ //id
                res = abi.encodePacked(keccak256(input[tag : t]));
            } else { //哈希值
                res = input[tag : t];
            }
            tag = t + 1;
            return (res, tag);
        }
        bytes memory chdHash;
        for (uint c = 0; c < 4; ++ c) {
            bytes memory subHash;
            (subHash, tag) = computeRootHash(input, tag);
            chdHash = abi.encodePacked(chdHash, subHash);
        }
        res = abi.encodePacked(keccak256(chdHash));
        return (res, tag);
    }

    function stringToBytes(string memory input) pure public returns(bytes memory res, uint len) {
        res = bytes(input);
        return (res, res.length);
    }
}
